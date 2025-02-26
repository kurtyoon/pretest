package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;
import dev.kurtyoon.pretest.application.port.out.ExcelParserPort;
import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import dev.kurtyoon.pretest.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateBulkOrderServiceTest {

    @Mock
    private ExcelParserPort excelParserPort;

    @Mock
    private LockPort lockPort;

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    private CreateBulkOrderService createBulkOrderService;

    private byte[] mockExcelData;

    @BeforeEach
    void setUp() {
        createBulkOrderService = new CreateBulkOrderService(
                excelParserPort,
                productRepositoryPort,
                orderRepositoryPort,
                lockPort
        );
        mockExcelData = "test-excel-data".getBytes();
    }

    @Test
    @DisplayName("엑셀 데이터가 비어있을 경우 빈 결과 반환")
    void returnEmptyResultForEmptyExcelData() {
        // Given
        when(excelParserPort.parse(mockExcelData)).thenReturn(List.of());

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        assertThat(result.getSuccessOrders()).isEmpty();
        assertThat(result.getFailedOrders()).isEmpty();
        verify(lockPort, never()).lock(anyString());
        verify(lockPort, never()).unlock(anyString());
    }

    @Test
    @DisplayName("주문 내 중복 상품이 있는 경우 예외 발생")
    void throwExceptionForDuplicateProductsInOrder() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(1L, "상품1", 3) // 상품 ID 중복
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // When & Then
        CommonException exception = assertThrows(CommonException.class,
                () -> createBulkOrderService.execute(mockExcelData));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PRODUCT_ORDER);
        verify(lockPort, never()).lock(anyString());
    }

    @Test
    @DisplayName("성공적인 대량 주문 처리")
    void processMultipleOrdersSuccessfully() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(2L, "상품2", 1),
                                new OrderItemCommand(3L, "상품3", 3)
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 모든 필요한 상품 ID 목록
        List<Long> expectedProductIds = List.of(1L, 2L, 3L);

        // 상품 Mock 설정
        List<Product> products = List.of(
                Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now()),
                Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now()),
                Product.create(3L, "상품3", 8, 3000, LocalDateTime.now(), LocalDateTime.now())
        );
        when(productRepositoryPort.findAllByIdList(expectedProductIds)).thenReturn(products);

        // 주문 저장 Mock 설정
        List<Order> savedOrders = List.of(
                Order.create(1L, "고객1", "서울시", List.of(
                        OrderItem.create(
                                1L, 1L, "상품1", 2, 2000
                        ),
                        OrderItem.create(
                                2L, 2L, "상품2", 1, 2000
                        )
                )),
                Order.create(2L, "고객2", "부산시", List.of(
                        OrderItem.create(
                                3L, 2L, "상품2", 1, 2000
                        ),
                        OrderItem.create(
                                4L, 3L, "상품3", 3, 9000
                        )
                ))
        );
        when(orderRepositoryPort.saveAllOrder(anyList())).thenReturn(savedOrders);

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // 락 획득 및 해제 검증
        verify(lockPort).lock("PRODUCT_LOCK:1");
        verify(lockPort).lock("PRODUCT_LOCK:2");
        verify(lockPort).lock("PRODUCT_LOCK:3");

        // 역순으로 락 해제 검증
        InOrder inOrder = inOrder(lockPort);
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:3");
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:2");
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:1");

        // 결과 검증
        assertThat(result.getSuccessOrders()).hasSize(commandList.size());
        assertThat(result.getFailedOrders()).isEmpty();

        // 상품 및 주문 저장 검증
        verify(productRepositoryPort).saveAllProducts(anyList());
        verify(orderRepositoryPort).saveAllOrder(anyList());
    }

    @Test
    @DisplayName("일부 주문 실패 케이스")
    void someOrdersFail() {
        // Given
        // 두 번째 주문은 재고가 부족하도록 설정
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 5), // 재고보다 많음 (재고: 3)
                                new OrderItemCommand(3L, "상품3", 2)
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 모든 필요한 상품 ID 목록
        List<Long> expectedProductIds = List.of(1L, 2L, 3L);

        // 상품 Mock 설정
        Product product1 = Product.create(1L, "상품1", 3, 1000, LocalDateTime.now(), LocalDateTime.now()); // 재고 3개
        Product product2 = Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());
        Product product3 = Product.create(3L, "상품3", 8, 3000, LocalDateTime.now(), LocalDateTime.now());
        when(productRepositoryPort.findAllByIdList(expectedProductIds))
                .thenReturn(List.of(product1, product2, product3));

        // 첫 번째 주문만 성공으로 설정
        List<Order> savedOrders = List.of(
                Order.create(1L, "고객1", "서울시", List.of(
                        OrderItem.create(1L, 1L, "상품1", 2, 2000),
                        OrderItem.create(2L, 2L, "상품2", 1, 2000)
                ))
        );
        when(orderRepositoryPort.saveAllOrder(anyList())).thenReturn(savedOrders);

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // 결과 검증
        assertThat(result.getSuccessOrders()).hasSize(1);
        assertThat(result.getFailedOrders()).hasSize(1);
        assertThat(result.getFailedOrders().get(0).getCustomerName()).isEqualTo("고객2");

        // 상품 및 주문 저장 검증
        verify(productRepositoryPort).saveAllProducts(anyList());
        verify(orderRepositoryPort).saveAllOrder(anyList());
    }

    @Test
    @DisplayName("전체 주문량이 재고를 초과하는 경우")
    void totalOrderQuantityExceedsStock() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(new OrderItemCommand(1L, "상품1", 3))),
                new OrderCommand("고객2", "부산시",
                        List.of(new OrderItemCommand(1L, "상품2", 5)))
                // 총 필요 수량: 8, 재고: 5
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 상품 Mock 설정
        Product product = Product.create(1L, "상품1", 5, 1000, LocalDateTime.now(), LocalDateTime.now()); // 재고 5개
        when(productRepositoryPort.findAllByIdList(List.of(1L)))
                .thenReturn(List.of(product));

        Order savedOrder = Order.create(1L, "고객1", "서울시", List.of(
                OrderItem.create(1L, 1L, "상품1", 3, 3000)
        ));
        when(orderRepositoryPort.saveAllOrder(any())).thenReturn(List.of(savedOrder));

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // Then
        // 첫 번째 주문은 성공
        assertThat(result.getSuccessOrders()).hasSize(1);
        assertThat(result.getSuccessOrders().get(0).getCustomerName()).isEqualTo("고객1");

        // 두 번째 주문은 재고 부족으로 실패
        assertThat(result.getFailedOrders()).hasSize(1);
        assertThat(result.getFailedOrders().get(0).getCustomerName()).isEqualTo("고객2");
        assertThat(result.getFailedOrders().get(0).getReason()).contains(ErrorCode.OUT_OF_STOCK.getMessage());

        // 서비스 호출 검증
        verify(lockPort).lock("PRODUCT_LOCK:1");
        verify(lockPort).unlock("PRODUCT_LOCK:1");
        verify(orderRepositoryPort).saveAllOrder(argThat(orders ->
                orders.size() == 1 && orders.get(0).getCustomerName().equals("고객1")));
        verify(productRepositoryPort).saveAllProducts(argThat(products ->
                products.size() == 1 && products.get(0).getQuantity() == 2)); // 5 - 3 = 2 남음
    }

    @Test
    @DisplayName("상품이 존재하지 않는 경우")
    void productNotFound() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(999L, "상품999", 1) // 존재하지 않는 상품
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 필요한 상품 중 하나만 존재
        when(productRepositoryPort.findAllByIdList(List.of(1L, 999L)))
                .thenReturn(List.of(
                        Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now())
                ));

        Order savedOrder = Order.create(1L, "고객1", "서울시", List.of(
                OrderItem.create(1L, 1L, "상품1", 2, 2000)
        ));
        when(orderRepositoryPort.saveAllOrder(any())).thenReturn(List.of(savedOrder));

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // 락은 획득 및 해제되었지만 주문 처리는 되지 않음
        assertThat(result.getSuccessOrders()).hasSize(1);
        assertThat(result.getSuccessOrders().get(0).getCustomerName()).isEqualTo("고객1");

        assertThat(result.getFailedOrders()).hasSize(1);
        assertThat(result.getFailedOrders().get(0).getCustomerName()).isEqualTo("고객2");
        assertThat(result.getFailedOrders().get(0).getReason()).contains(ErrorCode.NOT_FOUND_PRODUCT.getMessage());

        // 서비스 호출 검증
        verify(productRepositoryPort).findAllByIdList(List.of(1L, 999L));
        verify(productRepositoryPort).saveAllProducts(anyList());
        verify(orderRepositoryPort).saveAllOrder(argThat(orders ->
                orders.size() == 1 && orders.get(0).getCustomerName().equals("고객1")));
    }

    @Test
    @DisplayName("락 획득 실패 케이스")
    void lockAcquisitionFailure() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(2L, "상품2", 1),
                                new OrderItemCommand(3L, "상품3", 3)
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 두 번째 락 획득 실패 시뮬레이션
        doNothing().when(lockPort).lock("PRODUCT_LOCK:1");
        doThrow(new RuntimeException("Lock acquisition failed")).when(lockPort).lock("PRODUCT_LOCK:2");

        // When & Then
        assertThrows(RuntimeException.class, () -> createBulkOrderService.execute(mockExcelData));

        // 첫 번째 락은 해제되었는지 검증
        verify(lockPort).unlock("PRODUCT_LOCK:1");

        // 상품 조회 및 주문 처리가 되지 않았는지 검증
        verify(productRepositoryPort, never()).findAllByIdList(anyList());
        verify(orderRepositoryPort, never()).saveAllOrder(anyList());
    }

    @Test
    @DisplayName("락 해제 실패 시에도 다른 락은 정상 해제")
    void unlockContinuesEvenIfOneFails() {
        // Given
        List<OrderCommand> commandList =  List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(2L, "상품2", 1),
                                new OrderItemCommand(3L, "상품3", 3)
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 모든 필요한 상품 ID 목록
        List<Long> expectedProductIds = List.of(1L, 2L, 3L);

        // 상품 Mock 설정
        List<Product> products = List.of(
                Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now()),
                Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now()),
                Product.create(3L, "상품3", 8, 3000, LocalDateTime.now(), LocalDateTime.now())
        );
        when(productRepositoryPort.findAllByIdList(expectedProductIds)).thenReturn(products);

        // 주문 저장 Mock 설정
        List<Order> savedOrders = List.of(
                Order.create(1L, "고객1", "서울시", List.of(
                        OrderItem.create(
                                1L, 1L, "상품1", 2, 2000
                        ),
                        OrderItem.create(
                                2L, 2L, "상품2", 1, 2000
                        )
                )),
                Order.create(2L, "고객2", "부산시", List.of(
                        OrderItem.create(
                                3L, 2L, "상품2", 1, 2000
                        ),
                        OrderItem.create(
                                4L, 3L, "상품3", 3, 9000
                        )
                ))
        );
        when(orderRepositoryPort.saveAllOrder(anyList())).thenReturn(savedOrders);

        // 두 번째 락 해제 실패 시뮬레이션
        doThrow(new RuntimeException("Unlock failed")).when(lockPort).unlock("PRODUCT_LOCK:2");

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // 모든 락에 대한 해제 시도가 있었는지 검증
        verify(lockPort).unlock("PRODUCT_LOCK:3");
        verify(lockPort).unlock("PRODUCT_LOCK:2"); // 실패하지만 호출됨
        verify(lockPort).unlock("PRODUCT_LOCK:1");

        // 주문이 정상적으로 처리되었는지 검증
        assertThat(result.getSuccessOrders()).hasSize(commandList.size());
    }

    @Test
    @DisplayName("모든 주문 실패시 결과 반환")
    void allOrdersFail() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(new OrderItemCommand(1L, "상품1", 6))), // 재고보다 많음
                new OrderCommand("고객2", "부산시",
                        List.of(new OrderItemCommand(1L, "상품1", 7))) // 재고보다 많음
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 상품 Mock 설정
        Product product = Product.create(1L, "상품1", 5, 1000, LocalDateTime.now(), LocalDateTime.now()); // 재고 5개
        when(productRepositoryPort.findAllByIdList(List.of(1L)))
                .thenReturn(List.of(product));

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        // 모든 주문은 실패해야함
        assertThat(result.getFailedOrders()).hasSize(2);

        // 주문은 저장되어서는 안됨
        verify(orderRepositoryPort, never()).saveAllOrder(anyList());
    }

    @Test
    @DisplayName("개별 주문 처리 중 재고 부족 발생")
    void individualOrderOutOfStock() {
        // Given
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(2L, "상품2", 5) // 첫 주문 후 재고 부족 발생
                        ))
        );
        when(excelParserPort.parse(mockExcelData)).thenReturn(commandList);

        // 상품 Mock 설정
        Product product1 = Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());
        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L)))
                .thenReturn(List.of(product1, product2));

        // 첫 번째 주문만 성공으로 설정
        List<Order> savedOrders = List.of(
                Order.create(1L, "고객1", "서울시", List.of(
                        OrderItem.create(1L, 1L, "상품1", 2, 2000),
                        OrderItem.create(2L, 2L, "상품2", 1, 2000)
                ))
        );
        when(orderRepositoryPort.saveAllOrder(anyList())).thenReturn(savedOrders);

        // When
        BulkOrderResult result = createBulkOrderService.execute(mockExcelData);

        // Then
        assertThat(result.getSuccessOrders()).hasSize(1);
        assertThat(result.getFailedOrders()).hasSize(1);
    }

    @Test
    @DisplayName("Thread-safe 테스트 - 두 스레드에서 동시에 BulkOrder 실행")
    void concurrentBulkOrderExecution() throws InterruptedException {
        // Given
        List<OrderCommand> commandList1 = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(new OrderItemCommand(1L, "상품1", 2)))
        );
        List<OrderCommand> commandList2 = List.of(
                new OrderCommand("고객2", "부산시",
                        List.of(new OrderItemCommand(1L, "상품1", 3)))
        );

        byte[] excelData1 = "excel1".getBytes();
        byte[] excelData2 = "excel2".getBytes();

        when(excelParserPort.parse(excelData1)).thenReturn(commandList1);
        when(excelParserPort.parse(excelData2)).thenReturn(commandList2);

        // 상품 Mock 설정 - 두 스레드 모두 동일한 상품 사용
        Product product = Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L)))
                .thenReturn(List.of(product));

        when(orderRepositoryPort.saveAllOrder(any()))
                .thenAnswer(invocation -> {
                    List<Order> orders = invocation.getArgument(0);

                    if (orders == null || orders.isEmpty()) {
                        return List.of();
                    }

                    Order firstOrder = orders.get(0);

                    if ("고객1".equals(firstOrder.getCustomerName())) {
                        return List.of(Order.create(1L, "고객1", "서울시", List.of(
                                OrderItem.create(1L, 1L, "상품1", 2, 2000)
                        )));
                    } else if ("고객2".equals(firstOrder.getCustomerName())) {
                        return List.of(Order.create(2L, "고객2", "부산시", List.of(
                                OrderItem.create(2L, 1L, "상품1", 3, 3000)
                        )));
                    }

                    return orders;
                });

        // 스레드가 동시에 락을 획득하도록 시뮬레이션
        doAnswer(invocation -> {
            // 실제로는 첫 번째 스레드만 락 획득 성공, 두 번째 스레드는 대기
            String key = invocation.getArgument(0);
            if (key.equals("PRODUCT_LOCK:1")) {
                Thread.sleep(10); // 약간의 지연
            }
            return null;
        }).when(lockPort).lock(anyString());

        // When - 두 스레드에서 동시에 실행
        Thread thread1 = new Thread(() -> createBulkOrderService.execute(excelData1));
        Thread thread2 = new Thread(() -> createBulkOrderService.execute(excelData2));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then
        // 각 스레드가 락을 획득하고 해제했는지 검증
        verify(lockPort, times(2)).lock("PRODUCT_LOCK:1");
        verify(lockPort, times(2)).unlock("PRODUCT_LOCK:1");

        // 각 스레드가 상품을 저장했는지 검증
        verify(productRepositoryPort, times(2)).saveAllProducts(anyList());

        // 각 스레드가 주문을 저장했는지 검증
        verify(orderRepositoryPort, times(2)).saveAllOrder(anyList());
    }
}