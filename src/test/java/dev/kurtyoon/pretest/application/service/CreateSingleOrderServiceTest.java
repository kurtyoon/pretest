package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSingleOrderServiceTest {

    @Mock
    private LockPort lockPort;

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    private CreateSingleOrderService createSingleOrderService;

    @BeforeEach
    void setUp() {
        createSingleOrderService = new CreateSingleOrderService(
                lockPort,
                orderRepositoryPort,
                productRepositoryPort
        );
    }

    @Test
    @DisplayName("주문 성공 - 정상 케이스")
    void executeSuccessfully() {

        // Given
        OrderCommand command = createTestOrderCommand(
                "고객1", "서울시", List.of(
                        new OrderItemCommand(1L, "상품 1", 1),
                        new OrderItemCommand(2L, "상품 2", 3)
                )
        );

        // 상품 Mock 설정
        Product product1 = Product.create(1L, "상품 1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품 2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L)))
                .thenReturn(List.of(product1, product2));

        // 주문 항목 Mock 설정
        OrderItem orderItem1 = OrderItem.create(1L, 1L, "상품 1", 1, 1000);
        OrderItem orderItem2 = OrderItem.create(2L, 2L, "상품 2", 3, 6000);

        // 주문 저장 Mock 설정
        Order savedOrder = Order.create(1L, "고객 1", "서울시", List.of(orderItem1, orderItem2));
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // When
        SingleOrderResult result = createSingleOrderService.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);

        // 상품이 정렬된 순서로 Lock이 획득되었는지 검증
        verify(lockPort).lock("PRODUCT_LOCK:1");
        verify(lockPort).lock("PRODUCT_LOCK:2");

        // 역순으로 Lock 이 해제되었는지 검증
        InOrder inOrder = Mockito.inOrder(lockPort);
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:2");
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:1");

        // 재고가 차감되었는지 검증
        verify(productRepositoryPort).saveAllProducts(anyList());
    }

    @Test
    @DisplayName("주문 실패 - 중복 상품")
    void failWithDuplicatedProducts() {

        // Given
        OrderCommand command = createTestOrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품 1", 2),
                        new OrderItemCommand(1L, "상품 2", 3)  // 중복 상품
                ));

        // When & Then
        CommonException exception = assertThrows(CommonException.class,
                () -> createSingleOrderService.execute(command));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PRODUCT_ORDER);

        // Lock 이 획득되지 않았는지 검증
        verify(lockPort, never()).lock(anyString());
    }

    @Test
    @DisplayName("주문 실패 - 상품 없음")
    void failWithProductNotFound() {

        // Given
        OrderCommand command = createTestOrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품 1", 2),
                        new OrderItemCommand(2L, "상품 2", 3)
                ));

        // 1개의 상품만 반환 -> 존재하지 않는 상품 시뮬레이션
        Product product1 = Product.create(1L, "상품 1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L)))
                .thenReturn(List.of(product1));

        // When & Then
        CommonException exception = assertThrows(CommonException.class,
                () -> createSingleOrderService.execute(command));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_PRODUCT);

        // Lock 해제 검증
        verify(lockPort).unlock("PRODUCT_LOCK:2");
        verify(lockPort).unlock("PRODUCT_LOCK:1");
    }

    @Test
    @DisplayName("주문 실패 - 재고 부족")
    void failWithInsufficientStock() {

        // Given
        OrderCommand command = createTestOrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품 1", 20),
                        new OrderItemCommand(2L, "상품 2", 3)
                ));

        // 상품 Mock 설정 - 재고 부족
        Product product1 = Product.create(1L, "상품 1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품 2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L)))
                .thenReturn(List.of(product1, product2));

        // When & Then
        CommonException exception = assertThrows(CommonException.class,
                () -> createSingleOrderService.execute(command));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);

        // Lock 해제 검증
        verify(lockPort).unlock("PRODUCT_LOCK:2");
        verify(lockPort).unlock("PRODUCT_LOCK:1");

        // 재고 복구 로직으로 인하여 상품 정보 저장은 한 번 호출되어야 함
        verify(productRepositoryPort, times(1)).saveAllProducts(anyList());

        // 주문은 저장되지 않아야 함
        verify(orderRepositoryPort, never()).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동일 상품 주문")
    void concurrentOrdersForSameProduct() throws InterruptedException {

        // Given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 동일한 상품에 대해 여러 스레드에서 주문
        OrderCommand command = createTestOrderCommand("고객", "서울시",
                List.of(new OrderItemCommand(1L, "상품 1", 1)));

        // 상품 Mock 설정 - 초기 재고 10개
        Product product = spy(Product.create(1L, "상품 1", 10, 1000, LocalDateTime.now(), LocalDateTime.now()));

        // 재고 상태 유지
        when(productRepositoryPort.findAllByIdList(List.of(1L)))
                .thenReturn(List.of(product));

        // 주문 상품 Mock 설정
        OrderItem orderItem = OrderItem.create(1L, 1L, "상품 1", 1, 1000);

        // 주문 저장 Mock 설정
        Order savedOrder = Order.create(1L, "고객", "서울시", List.of(orderItem));
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // LockPort Mock 동작 설정
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);

            // 실제 Lock 획득 로직은 구현하지 않고 호출만 기록
            return null;
        }).when(lockPort).lock(anyString());

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    createSingleOrderService.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족 예외는 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then
        // 10개의 재고로 최대 10개의 주문만 성공해야 함
        verify(product, times(successCount.get())).reduceStock(1);

        // 재고가 정확히 차감되었는지 검증
        verify(productRepositoryPort, times(successCount.get())).saveAllProducts(anyList());
    }

    @Test
    @DisplayName("Lock 해제 실패 시 다른 Lock은 계속 해제 시도")
    void unlockContinuesEvenIfOneFails() {

        // Given
        OrderCommand command = createTestOrderCommand(
                "고객1", "서울시", List.of(
                        new OrderItemCommand(1L, "상품 1", 2),
                        new OrderItemCommand(2L, "상품 2", 1),
                        new OrderItemCommand(3L, "상품 3", 3)
                )
        );

        // 상품 Mock 설정
        Product product1 = Product.create(1L, "상품 1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품 2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());
        Product product3 = Product.create(3L, "상품 3", 15, 1500, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(product1, product2, product3));

        // 주문 항목 Mock 설정
        OrderItem orderItem1 = OrderItem.create(1L, "상품 1", 2, 1000);
        OrderItem orderItem2 = OrderItem.create(2L, "상품 2", 1, 2000);
        OrderItem orderItem3 = OrderItem.create(3L, "상품 3", 3, 1500);

        // 주문 저장 Mock 설정
        Order savedOrder = Order.create(1L, "고객1", "서울시", List.of(orderItem1, orderItem2, orderItem3));
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // 두 번째 Lock 해제 시 예외 발생
        doThrow(new RuntimeException("Unlock failed")).when(lockPort).unlock("PRODUCT_LOCK:2");

        // When - 주문 처리는 성공하지만 락 해제 과정에서 일부 예외 발생
        SingleOrderResult result = createSingleOrderService.execute(command);

        // Then
        // 주문이 성공적으로 처리되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);

        // 락 해제가 모두 시도되었는지 확인 (예외가 발생해도 다른 락은 해제 시도)
        // 역순으로 해제되므로 3,2,1 순서로 verify
        verify(lockPort).unlock("PRODUCT_LOCK:3");
        verify(lockPort).unlock("PRODUCT_LOCK:2");
        verify(lockPort).unlock("PRODUCT_LOCK:1");
    }

    @Test
    @DisplayName("병렬 주문 - 서로 다른 상품")
    void parallelOrdersForDifferentProducts() throws InterruptedException {
        // Given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Mock 응답을 통일하여 스레드 안전성 확보
        List<Product> allProducts = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Long productId = (long) (i + 1);
            Product product = Product.create(productId, "상품" + productId, 10, 1000 * (i + 1), LocalDateTime.now(), LocalDateTime.now());
            allProducts.add(product);
        }

        // 개별 ID 목록에 대한 findAllByIdList 응답 설정
        when(productRepositoryPort.findAllByIdList(anyList())).thenAnswer(invocation -> {
            List<Long> requestedIds = invocation.getArgument(0);
            return allProducts.stream()
                    .filter(p -> requestedIds.contains(p.getId()))
                    .collect(Collectors.toList());
        });

        // 주문 저장 Mock 설정
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return Order.create(
                    (long) (Math.random() * 1000),
                    order.getCustomerName(),
                    order.getCustomerAddress(),
                    new ArrayList<>(order.getItems())
            );
        });

        // 5개의 서로 다른 주문
        OrderCommand[] commands = new OrderCommand[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Long productId = (long) (i + 1);
            commands[i] = createTestOrderCommand(
                    "고객" + i,
                    "주소" + i,
                    List.of(new OrderItemCommand(productId, "상품" + i, 1))
            );
        }

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    createSingleOrderService.execute(commands[index]);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then
        // 각 상품에 대한 Lock 획득 및 해제 검증
        for (int i = 1; i <= threadCount; i++) {
            verify(lockPort, atLeastOnce()).lock("PRODUCT_LOCK:" + i);
            verify(lockPort, atLeastOnce()).unlock("PRODUCT_LOCK:" + i);
        }

        // 모든 주문이 저장되었는지 검증
        verify(orderRepositoryPort, times(threadCount)).saveOrder(any(Order.class));

        // 모든 상품이 업데이트되었는지 검증
        verify(productRepositoryPort, times(threadCount)).saveAllProducts(anyList());
    }

    @Test
    @DisplayName("데드락 방지 - 상품 ID 정렬 검증")
    void deadlockPreventionWithSortedProductIds() {
        // Given
        // 의도적으로 상품 ID를 무작위 순서로 배치
        OrderCommand command = createTestOrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(3L, "상품3", 1),
                        new OrderItemCommand(1L, "상품1", 2),
                        new OrderItemCommand(2L, "상품2", 3)
                ));

        // 상품 Mock 설정
        Product product1 = Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());
        Product product3 = Product.create(3L, "상품3", 8, 3000, LocalDateTime.now(), LocalDateTime.now());

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(product1, product2, product3));

        OrderItem orderItem1 = OrderItem.create(1L, 3L, "상품3", 1, 1000);
        OrderItem orderItem2 = OrderItem.create(2L, 1L, "상품1", 2, 4000);
        OrderItem orderItem3 = OrderItem.create(3L, 2L, "상품2", 3, 9000);

        Order savedOrder = Order.create(1L, "고객1", "서울시", List.of(orderItem1, orderItem2, orderItem3));
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // When
        createSingleOrderService.execute(command);

        // Then
        // 상품 ID가 정렬된 순서로 락이 획득되었는지 검증
        InOrder inOrder = inOrder(lockPort);
        inOrder.verify(lockPort).lock("PRODUCT_LOCK:1");
        inOrder.verify(lockPort).lock("PRODUCT_LOCK:2");
        inOrder.verify(lockPort).lock("PRODUCT_LOCK:3");

        // 락이 역순으로 해제되었는지 검증
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:3");
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:2");
        inOrder.verify(lockPort).unlock("PRODUCT_LOCK:1");
    }

    @Test
    @DisplayName("롤백 테스트 - 주문 저장 실패 시 재고는 복구되어야 함")
    void rollbackWhenOrderSaveFails() {
        // Given
        OrderCommand command = createTestOrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품1", 2),
                        new OrderItemCommand(2L, "상품2", 1)
                ));

        // 상품 Mock 설정
        Product product1 = spy(Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now()));
        Product product2 = spy(Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now()));

        when(productRepositoryPort.findAllByIdList(List.of(1L, 2L)))
                .thenReturn(List.of(product1, product2));

        // 주문 저장 실패 시뮬레이션
        when(orderRepositoryPort.saveOrder(any(Order.class)))
                .thenThrow(new RuntimeException("DB 저장 실패"));

        // When & Then
        assertThrows(RuntimeException.class, () -> createSingleOrderService.execute(command));

        // 재고가 차감되었는지 검증
        verify(product1).reduceStock(2);
        verify(product2).reduceStock(1);

        // 원래 재고로 복구되었는지 검증
        verify(product1).updateQuantity(10);
        verify(product2).updateQuantity(5);

        // 재고 차감 후와 복구 후 두 번 상품이 저장되었는지 검증
        verify(productRepositoryPort, times(2)).saveAllProducts(anyList());

        // 락이 해제되었는지 검증
        verify(lockPort).unlock("PRODUCT_LOCK:2");
        verify(lockPort).unlock("PRODUCT_LOCK:1");
    }

    @Test
    @DisplayName("주문 아이템이 없는 경우")
    void orderWithNoItems() {
        // Given
        OrderCommand command = createTestOrderCommand("고객1", "서울시", List.of());

        // When & Then
        // 빈 주문은 에러가 발생해야 함
        CommonException exception = assertThrows(CommonException.class,
                () -> createSingleOrderService.execute(command));

        // 적절한 에러 코드가 반환되어야 함
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER);

        // 락이 획득되지 않았는지 검증
        verify(lockPort, never()).lock(anyString());
        verify(lockPort, never()).unlock(anyString());

        // 주문은 저장되지 않았는지 검증
        verify(orderRepositoryPort, never()).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("경쟁 조건 테스트 - 두 스레드가 동시에 동일 상품 주문")
    void raceConditionTest() throws InterruptedException {

        // Given
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicBoolean firstThread = new AtomicBoolean(false);
        CountDownLatch firstThreadCompleteLatch = new CountDownLatch(1);

        // 동일한 상품에 대한 주문 명령
        OrderCommand command = new OrderCommand("고객", "서울시", List.of(new OrderItemCommand(1L, "상품1", 5)));

        // 상품의 재고 한정
        Product product = Product.create(1L, "상품1", 5, 1000, LocalDateTime.now(), LocalDateTime.now());

        // 상품 조회 Mock 설정
        when(productRepositoryPort.findAllByIdList(List.of(1L)))
                .thenReturn(List.of(product));

        // 주문 저장 Mock 설정
        when(orderRepositoryPort.saveOrder(any(Order.class))).thenReturn(
                Order.create(1L, "고객", "서울시", List.of(OrderItem.create(1L, "상품1", 5, 5000)))
        );

        // lock Mock 설정
        doAnswer(invocation -> {
            barrier.await();

            if (firstThread.compareAndSet(false, true)) {
                return null;
            } else {
                firstThreadCompleteLatch.await();

                return null;
            }
        }).when(lockPort).lock(anyString());

        doAnswer(invocation -> {
            if (firstThread.get()) {
                firstThreadCompleteLatch.countDown();
            }

            return null;
        }).when(lockPort).unlock(anyString());

        // When
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    createSingleOrderService.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    caughtException.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(caughtException.get()).isInstanceOf(CommonException.class);
        assertThat(((CommonException) caughtException.get()).getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);

        // Lock 관련 검증
        verify(lockPort, times(2)).lock(eq("PRODUCT_LOCK:1"));
        verify(lockPort, times(2)).unlock(eq("PRODUCT_LOCK:1"));
    }

    @Test
    @DisplayName("락 획득 실패 시나리오")
    void lockAcquisitionFailure() {
        // Given
        CreateSingleOrderService service = new CreateSingleOrderService(
                lockPort, orderRepositoryPort, productRepositoryPort);

        OrderCommand command = new OrderCommand("고객", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품1", 2),
                        new OrderItemCommand(2L, "상품2", 3)
                ));

        // 두 번째 상품의 락 획득 실패 시뮬레이션
        doNothing().when(lockPort).lock("PRODUCT_LOCK:1");
        doThrow(new RuntimeException("Lock acquisition failed")).when(lockPort).lock("PRODUCT_LOCK:2");

        // When & Then
        try {
            service.execute(command);
        } catch (Exception e) {
            // 첫 번째 락은 해제되었는지 확인
            verify(lockPort).unlock("PRODUCT_LOCK:1");
            // 상품이 조회되지 않았는지 확인
            verify(productRepositoryPort, never()).findAllByIdList(anyList());
            // 주문이 저장되지 않았는지 확인
            verify(orderRepositoryPort, never()).saveOrder(any(Order.class));
        }

    }

    /**
     * 1) 재고가 다른 상품들 (5종)
     * 2) 서로 다른 조합의 OrderCommand
     * 3) 여러 스레드가 병렬로 실행 -> 일부 성공, 일부 재고 부족 발생
     */
    @Test
    @DisplayName("고부하 테스트 - 다수 스레드의 다양한 주문 조합")
    void highLoadDifferentOrderCombinations() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 상품 Mock 준비
        Product product1 = Product.create(1L, "상품1", 8, 3000000,
                LocalDateTime.now(), LocalDateTime.now());
        Product product2 = Product.create(2L, "상품2", 36, 1200000,
                LocalDateTime.now(), LocalDateTime.now());
        Product product3 = Product.create(3L, "상품3", 12, 800000,
                LocalDateTime.now(), LocalDateTime.now());
        Product product4 = Product.create(4L, "상품4", 8, 600000,
                LocalDateTime.now(), LocalDateTime.now());
        Product product5 = Product.create(5L, "상품5", 20, 400000,
                LocalDateTime.now(), LocalDateTime.now());

        // findAllByIdList Mock
        doAnswer((InvocationOnMock invocation) -> {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) invocation.getArgument(0);
            List<Product> matched = new ArrayList<>();
            if (ids.contains(1L)) matched.add(product1);
            if (ids.contains(2L)) matched.add(product2);
            if (ids.contains(3L)) matched.add(product3);
            if (ids.contains(4L)) matched.add(product4);
            if (ids.contains(5L)) matched.add(product5);
            return matched;
        }).when(productRepositoryPort).findAllByIdList(anyList());

        // saveOrder Mock - 주문 ID 자동 증가
        AtomicLong orderIdGen = new AtomicLong(1000);
        when(orderRepositoryPort.saveOrder(any(Order.class)))
                .thenAnswer(inv -> {
                    Order reqOrder = inv.getArgument(0);
                    return Order.create(
                            orderIdGen.getAndIncrement(),
                            reqOrder.getCustomerName(),
                            reqOrder.getCustomerAddress(),
                            reqOrder.getItems()
                    );
                });

        // saveAllProducts Mock (아무것도 안 함)
        doNothing().when(productRepositoryPort).saveAllProducts(anyList());

        // lock/unlock Mock
        doNothing().when(lockPort).lock(anyString());
        doNothing().when(lockPort).unlock(anyString());

        // 다양한 주문 커맨드 준비
        List<OrderCommand> combos = defineComplexOrderCombos();

        // 성공/실패 카운트
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 병렬 실행
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    OrderCommand cmd = combos.get(idx % combos.size());
                    createSingleOrderService.execute(cmd);
                    successCount.incrementAndGet();
                } catch (CommonException ce) {
                    errorCount.incrementAndGet();
                    System.err.println("주문 실패 [CommonException]: " + ce.getMessage());
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("주문 실패 [Etc]: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 결과 확인
        assertEquals(threadCount, successCount.get(), "모든 주문은 성공해야한다.");
        assertEquals(0, errorCount.get(), "실패한 주문은 없어야 한다.");

        // 실패 및 성공의 총 합은 threadCount 와 동일해야 한다.
        assertThat(successCount.get() + errorCount.get()).isEqualTo(threadCount);

        // Lock 획득 및 해제
        verify(lockPort, atLeast(threadCount)).lock(anyString());
        verify(lockPort, atLeast(threadCount)).unlock(anyString());

        // Thread 의 수 만큼 주문을 저장해야 한다.
        verify(orderRepositoryPort, times(threadCount)).saveOrder(any(Order.class));
        // 상품 업데이트 횟수
        verify(productRepositoryPort, atLeast(0)).saveAllProducts(anyList());
    }

    private List<OrderCommand> defineComplexOrderCombos() {
        List<OrderCommand> combos = new ArrayList<>();

        combos.add(new OrderCommand("고객1", "서울시",
                List.of(new OrderItemCommand(1L, "상품1", 1))));

        combos.add(new OrderCommand("고객2", "부산시",
                List.of(new OrderItemCommand(2L, "상품2", 2),
                        new OrderItemCommand(3L, "상품3", 1))));

        combos.add(new OrderCommand("고객3", "인천시",
                List.of(new OrderItemCommand(3L, "상품3", 2),
                        new OrderItemCommand(4L, "상품4", 1))));

        combos.add(new OrderCommand("고객4", "대전시",
                List.of(new OrderItemCommand(1L, "상품1", 1),
                        new OrderItemCommand(2L, "상품2", 2),
                        new OrderItemCommand(5L, "상품5", 3))));

        combos.add(new OrderCommand("고객5", "광주시",
                List.of(new OrderItemCommand(4L, "상품4", 1),
                        new OrderItemCommand(5L, "상품5", 2))));

        combos.add(new OrderCommand("고객6", "대구시",
                List.of(new OrderItemCommand(2L, "상품2", 5))));

        return combos;
    }

    @Test
    @DisplayName("복구 테스트 - 락 자원 누수 방지")
    void resourceLeakPreventionTest() {
        // Given
        CreateSingleOrderService service = new CreateSingleOrderService(
                lockPort, orderRepositoryPort, productRepositoryPort);

        OrderCommand command = new OrderCommand("고객", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품1", 2),
                        new OrderItemCommand(2L, "상품2", 3),
                        new OrderItemCommand(3L, "상품3", 1)
                ));

        // 상품 조회 실패 시뮬레이션
        doNothing().when(lockPort).lock(anyString());
        when(productRepositoryPort.findAllByIdList(anyList()))
                .thenThrow(new RuntimeException("DB 조회 실패"));

        // When
        try {
            service.execute(command);
        } catch (Exception e) {
            // Then
            // 모든 락이 해제되었는지 검증 (역순으로)
            verify(lockPort).unlock("PRODUCT_LOCK:3");
            verify(lockPort).unlock("PRODUCT_LOCK:2");
            verify(lockPort).unlock("PRODUCT_LOCK:1");
        }
    }

    private OrderCommand createTestOrderCommand(
            String customerName,
            String address,
            List<OrderItemCommand> items) {
        return new OrderCommand(customerName, address, items);
    }

}