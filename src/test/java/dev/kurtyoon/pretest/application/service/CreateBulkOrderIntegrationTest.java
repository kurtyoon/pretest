package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;
import dev.kurtyoon.pretest.application.port.out.ExcelParserPort;
import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateBulkOrderIntegrationTest {
    private CreateBulkOrderService createBulkOrderService;

    // 테스트를 위한 인메모리 구현체
    private TestExcelParserPort excelParserPort;
    private TestLockPort lockPort;
    private TestOrderRepositoryPort orderRepositoryPort;
    private TestProductRepositoryPort productRepositoryPort;

    @BeforeEach
    void setUp() {
        excelParserPort = new TestExcelParserPort();
        lockPort = new TestLockPort();
        orderRepositoryPort = new TestOrderRepositoryPort();
        productRepositoryPort = new TestProductRepositoryPort();

        createBulkOrderService = new CreateBulkOrderService(
                excelParserPort,
                productRepositoryPort,
                orderRepositoryPort,
                lockPort
        );

        // 테스트용 상품 데이터 초기화
        Product product1 = Product.create(1L, "상품1", 10, 1000, LocalDateTime.now(), LocalDateTime.now());

        Product product2 = Product.create(2L, "상품2", 5, 2000, LocalDateTime.now(), LocalDateTime.now());

        Product product3 = Product.create(3L, "상품3", 8, 3000, LocalDateTime.now(), LocalDateTime.now());

        productRepositoryPort.saveProduct(product1);
        productRepositoryPort.saveProduct(product2);
        productRepositoryPort.saveProduct(product3);
    }

    @Test
    @DisplayName("대량 주문 통합 테스트 - 정상 처리")
    void bulkOrderIntegrationTest() {
        // Given
        byte[] excelData = "test-excel-data".getBytes();
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
        excelParserPort.setParseResult(excelData, commandList);

        // When
        BulkOrderResult result = createBulkOrderService.execute(excelData);

        // Then
        // 주문이 성공적으로 처리되었는지 검증
        assertThat(result.getSuccessOrders()).hasSize(2);
        assertThat(result.getFailedOrders()).isEmpty();

        // 재고가 차감되었는지 검증
        Product product1 = productRepositoryPort.findById(1L);
        Product product2 = productRepositoryPort.findById(2L);
        Product product3 = productRepositoryPort.findById(3L);

        assertThat(product1.getQuantity()).isEqualTo(8); // 10 - 2
        assertThat(product2.getQuantity()).isEqualTo(3); // 5 - 1 - 1
        assertThat(product3.getQuantity()).isEqualTo(5); // 8 - 3

        // 락이 획득되고 해제되었는지 검증
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:1")).isEqualTo(1);
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:2")).isEqualTo(1);
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:3")).isEqualTo(1);
        assertThat(lockPort.getUnlockCount("PRODUCT_LOCK:1")).isEqualTo(1);
        assertThat(lockPort.getUnlockCount("PRODUCT_LOCK:2")).isEqualTo(1);
        assertThat(lockPort.getUnlockCount("PRODUCT_LOCK:3")).isEqualTo(1);

        // 주문이 저장되었는지 검증
        List<Order> savedOrders = orderRepositoryPort.getSavedOrders();
        assertThat(savedOrders).hasSize(2);
    }

    @Test
    @DisplayName("동시성 테스트 - 다수 스레드에서 동일 Excel 데이터 처리")
    void concurrentBulkOrderProcessing() throws InterruptedException {
        // Given
        byte[] excelData = "test-excel-data".getBytes();
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(new OrderItemCommand(1L, "상품1", 2))),
                new OrderCommand("고객2", "부산시",
                        List.of(new OrderItemCommand(1L, "상품1", 3)))
        );
        excelParserPort.setParseResult(excelData, commandList);

        int threadCount = 3; // 여러 스레드에서 동일한 Excel 데이터 처리
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    createBulkOrderService.execute(excelData);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then
        // 각 스레드가 락을 올바르게 획득하고 해제했는지 검증
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:1")).isEqualTo(threadCount);
        assertThat(lockPort.getUnlockCount("PRODUCT_LOCK:1")).isEqualTo(threadCount);

        // 재고가 정확히 차감되었는지 검증
        Product product = productRepositoryPort.findById(1L);
        assertThat(product.getQuantity()).isEqualTo(0); // 10개의 재고에 대해 5개의 수량 주문 요청이 3개이므로 0

        // 저장된 주문 수 검증
        assertThat(orderRepositoryPort.getSavedOrders().size()).isEqualTo(4); // 성공한 주문은 2개여야 함
    }

    @Test
    @DisplayName("동시성 테스트 - 2개 스레드의 동일 상품 대량 주문")
    void concurrentOrdersSameProduct() throws InterruptedException {
        // Given
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 스레드 1: 상품1을 6개 주문
        byte[] excelData1 = "excel1".getBytes();
        List<OrderCommand> commands1 = List.of(
                new OrderCommand("고객1", "서울시", List.of(new OrderItemCommand(1L, "상품1", 6)))
        );
        excelParserPort.setParseResult(excelData1, commands1);

        // 스레드 2: 상품1을 7개 주문
        byte[] excelData2 = "excel2".getBytes();
        List<OrderCommand> commands2 = List.of(
                new OrderCommand("고객2", "부산시", List.of(new OrderItemCommand(1L, "상품2", 7)))
        );
        excelParserPort.setParseResult(excelData2, commands2);

        // 상품1의 초기 재고는 10개

        // When
        executorService.submit(() -> {
            try {
                createBulkOrderService.execute(excelData1);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                createBulkOrderService.execute(excelData2);
            } finally {
                latch.countDown();
            }
        });

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then
        // 두 스레드 중 하나만 성공해야 함 (재고가 10개이므로)
        Product product = productRepositoryPort.findById(1L);
        if (product.getQuantity() == 4) {
            // 첫 번째 스레드가 성공한 경우 (10 - 6 = 4)
            assertThat(orderRepositoryPort.getSavedOrders()).hasSize(1);
            assertThat(orderRepositoryPort.getSavedOrders().get(0).getCustomerName()).isEqualTo("고객1");
        } else if (product.getQuantity() == 3) {
            // 두 번째 스레드가 성공한 경우 (10 - 7 = 3)
            assertThat(orderRepositoryPort.getSavedOrders()).hasSize(1);
            assertThat(orderRepositoryPort.getSavedOrders().get(0).getCustomerName()).isEqualTo("고객2");
        } else {
            // 둘 다 실패하거나 둘 다 성공한 경우 (발생하면 안 됨)
            assertThat(product.getQuantity()).isIn(3, 4);
        }

        // 락이 정확히 획득되고 해제되었는지 검증
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:1")).isEqualTo(2);
        assertThat(lockPort.getUnlockCount("PRODUCT_LOCK:1")).isEqualTo(2);
    }

    @Test
    @DisplayName("대량 주문 통합 테스트 - 일부 주문 실패")
    void bulkOrderWithPartialFailure() {
        // Given
        byte[] excelData = "test-excel-data".getBytes();
        List<OrderCommand> commandList = List.of(
                new OrderCommand("고객1", "서울시",
                        List.of(
                                new OrderItemCommand(1L, "상품1", 2),
                                new OrderItemCommand(2L, "상품2", 1)
                        )),
                new OrderCommand("고객2", "부산시",
                        List.of(
                                new OrderItemCommand(2L, "상품2", 5) // 재고 부족 (현재 5개, 첫 주문에서 1개 차감)
                        ))
        );
        excelParserPort.setParseResult(excelData, commandList);

        // When
        BulkOrderResult result = createBulkOrderService.execute(excelData);

        // Then
        // 첫 번째 주문만 성공
        assertThat(result.getSuccessOrders()).hasSize(1);
        assertThat(result.getFailedOrders()).hasSize(1);
        assertThat(result.getFailedOrders().get(0).getCustomerName()).isEqualTo("고객2");

        // 재고 확인
        assertThat(productRepositoryPort.findById(1L).getQuantity()).isEqualTo(8); // 10 - 2
        assertThat(productRepositoryPort.findById(2L).getQuantity()).isEqualTo(4); // 5 - 1

        // 주문 저장 확인
        assertThat(orderRepositoryPort.getSavedOrders()).hasSize(1);
        assertThat(orderRepositoryPort.getSavedOrders().get(0).getItems()).hasSize(2);
    }

    @Test
    @DisplayName("동시성 테스트 - 고부하 다양한 상품 주문")
    void highLoadConcurrentOrders() throws InterruptedException {
        // Given
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 5개의 서로 다른 엑셀 데이터
        for (int i = 0; i < 5; i++) {
            byte[] excelData = ("excel-" + i).getBytes();
            List<OrderCommand> commands = new ArrayList<>();

            // 각 엑셀에 4개의 주문 (다양한 상품 조합)
            commands.add(new OrderCommand("고객A" + i, "서울시",
                    List.of(new OrderItemCommand(1L, "상품1", 1))));

            commands.add(new OrderCommand("고객B" + i, "부산시",
                    List.of(new OrderItemCommand(2L, "상품2",1))));

            commands.add(new OrderCommand("고객C" + i, "대구시",
                    List.of(
                            new OrderItemCommand(1L, "상품1", 1),
                            new OrderItemCommand(2L, "상품2", 1)
                    )));

            commands.add(new OrderCommand("고객D" + i, "광주시",
                    List.of(new OrderItemCommand(3L, "상품3", 1))));

            excelParserPort.setParseResult(excelData, commands);
        }

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i % 5;
            byte[] excelData = ("excel-" + index).getBytes();

            executorService.submit(() -> {
                try {
                    createBulkOrderService.execute(excelData);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        // 각 상품의 재고가 0 이상인지 확인
        assertThat(productRepositoryPort.findById(1L).getQuantity()).isGreaterThanOrEqualTo(0);
        assertThat(productRepositoryPort.findById(2L).getQuantity()).isGreaterThanOrEqualTo(0);
        assertThat(productRepositoryPort.findById(3L).getQuantity()).isGreaterThanOrEqualTo(0);

        // 모든 락이 해제되었는지 확인
        assertThat(lockPort.isLocked("PRODUCT_LOCK:1")).isFalse();
        assertThat(lockPort.isLocked("PRODUCT_LOCK:2")).isFalse();
        assertThat(lockPort.isLocked("PRODUCT_LOCK:3")).isFalse();

        // 락 획득/해제 횟수가 일치하는지 확인
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:1")).isEqualTo(lockPort.getUnlockCount("PRODUCT_LOCK:1"));
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:2")).isEqualTo(lockPort.getUnlockCount("PRODUCT_LOCK:2"));
        assertThat(lockPort.getLockCount("PRODUCT_LOCK:3")).isEqualTo(lockPort.getUnlockCount("PRODUCT_LOCK:3"));
    }

    // 테스트용 구현체
    static class TestExcelParserPort implements ExcelParserPort {
        private final Map<String, List<OrderCommand>> parseResults = new HashMap<>();

        @Override
        public List<OrderCommand> parse(byte[] excelData) {
            return parseResults.getOrDefault(new String(excelData), List.of());
        }

        public void setParseResult(byte[] excelData, List<OrderCommand> commands) {
            parseResults.put(new String(excelData), commands);
        }
    }

    static class TestLockPort implements LockPort {
        private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

        private final Map<String, AtomicInteger> lockCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> unlockCounts = new ConcurrentHashMap<>();

        @Override
        public void lock(String key) {
            locks.computeIfAbsent(key, k -> new ReentrantLock()).lock();
            lockCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        }

        @Override
        public void unlock(String key) {
            ReentrantLock lock = locks.get(key);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                unlockCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            }
        }

        public int getLockCount(String key) {
            return lockCounts.getOrDefault(key, new AtomicInteger()).get();
        }

        public int getUnlockCount(String key) {
            return unlockCounts.getOrDefault(key, new AtomicInteger()).get();
        }

        public boolean isLocked(String key) {
            ReentrantLock lock = locks.get(key);
            return lock != null && lock.isLocked();
        }
    }

    static class TestOrderRepositoryPort implements OrderRepositoryPort {
        private final List<Order> orders = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong orderIdGenerator = new AtomicLong(1);

        @Override
        public Order saveOrder(Order order) {
            Order savedOrder = Order.create(
                    orderIdGenerator.getAndIncrement(),
                    order.getCustomerName(),
                    order.getCustomerAddress(),
                    new ArrayList<>(order.getItems())
            );
            orders.add(savedOrder);
            return savedOrder;
        }

        @Override
        public List<Order> saveAllOrder(List<Order> orderList) {
            List<Order> savedOrders = new ArrayList<>();
            for (Order order : orderList) {
                savedOrders.add(saveOrder(order));
            }
            return savedOrders;
        }

        public List<Order> getSavedOrders() {
            return new ArrayList<>(orders);
        }
    }

    static class TestProductRepositoryPort implements ProductRepositoryPort {
        private final Map<Long, Product> products = new ConcurrentHashMap<>();

        @Override
        public List<Product> findAllByIdList(List<Long> idList) {
            return idList.stream()
                    .map(this::findById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        @Override
        public void saveAllProducts(List<Product> productList) {
            for (Product product : productList) {
                saveProduct(product);
            }
        }

        public Product findById(Long id) {
            return products.get(id);
        }

        public void saveProduct(Product product) {
            // 동시성 테스트를 위해 같은 인스턴스를 계속 사용
            products.put(product.getId(), product);
        }
    }
}
