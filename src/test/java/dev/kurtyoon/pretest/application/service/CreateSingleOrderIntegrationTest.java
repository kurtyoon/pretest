package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.core.exception.CommonException;
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

public class CreateSingleOrderIntegrationTest {

    private CreateSingleOrderService createSingleOrderService;

    // 테스트를 위한 인메모리 구현체
    private TestLockPort lockPort;
    private TestOrderRepositoryPort orderRepositoryPort;
    private TestProductRepositoryPort productRepositoryPort;

    @BeforeEach
    void setUp() {
        lockPort = new TestLockPort();
        orderRepositoryPort = new TestOrderRepositoryPort();
        productRepositoryPort = new TestProductRepositoryPort();

        createSingleOrderService = new CreateSingleOrderService(
                lockPort,
                orderRepositoryPort,
                productRepositoryPort
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
    @DisplayName("동시성 테스트 - 실제 락을 사용하여 여러 스레드에서 동일 상품 주문")
    void concurrentOrdersWithRealLocks() throws InterruptedException {

        // Given
        int threadCount = 15; // 재고보다 많은 스레드 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 동일한 상품에 대해 여러 스레드에서 주문
        OrderCommand command = new OrderCommand("고객", "서울시",
                List.of(new OrderItemCommand(1L, "상품1", 1)));

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    createSingleOrderService.execute(command);
                    successCount.incrementAndGet();
                } catch (CommonException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then
        // 상품1의 재고는 10개이므로 10개의 주문만 성공해야 함
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);

        // 상품의 재고가 0이 되었는지 검증
        Product product = productRepositoryPort.findById(1L);
        assertThat(product.getQuantity()).isEqualTo(0);

        // Lock 이 모두 해제되었는지 검증
        assertThat(lockPort.getActiveLocks()).isEmpty();

        // 성공한 주문 수만큼 주문이 저장되었는지 검증
        assertThat(orderRepositoryPort.getSavedOrders().size()).isEqualTo(10);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 상품 주문 성공")
    void orderMultipleProductsSuccessfully() {

        // Given
        OrderCommand command = new OrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품2", 2),
                        new OrderItemCommand(2L, "상품3", 3)
                ));

        // When
        createSingleOrderService.execute(command);

        // Then
        // 재고가 차감되었는지 검증
        Product product1 = productRepositoryPort.findById(1L);
        Product product2 = productRepositoryPort.findById(2L);

        assertThat(product1.getQuantity()).isEqualTo(8); // 10 - 2
        assertThat(product2.getQuantity()).isEqualTo(2); // 5 - 3

        // 주문이 저장되었는지 검증
        List<Order> savedOrders = orderRepositoryPort.getSavedOrders();
        assertThat(savedOrders).hasSize(1);

        Order savedOrder = savedOrders.get(0);
        assertThat(savedOrder.getCustomerName()).isEqualTo("고객1");
        assertThat(savedOrder.getCustomerAddress()).isEqualTo("서울시");
        assertThat(savedOrder.getItems()).hasSize(2);

        // 락이 모두 해제되었는지 검증
        assertThat(lockPort.getActiveLocks()).isEmpty();
    }

    @Test
    @DisplayName("통합 테스트 - 재고 부족으로 주문 실패")
    void orderFailsDueToInsufficientStock() {

        // Given
        OrderCommand command = new OrderCommand("고객1", "서울시",
                List.of(
                        new OrderItemCommand(1L, "상품1", 2),
                        new OrderItemCommand(2L, "상품2", 6) // 재고보다 많음
                ));

        // When
        try {
            createSingleOrderService.execute(command);
        } catch (CommonException e) {
            // Then
            // 재고가 차감되지 않았는지 검증
            Product product1 = productRepositoryPort.findById(1L);
            Product product2 = productRepositoryPort.findById(2L);

            assertThat(product1.getQuantity()).isEqualTo(10); // 재고 변동 없음
            assertThat(product2.getQuantity()).isEqualTo(5);  // 재고 변동 없음

            // 주문이 저장되지 않았는지 검증
            assertThat(orderRepositoryPort.getSavedOrders()).isEmpty();

            // 락이 모두 해제되었는지 검증
            assertThat(lockPort.getActiveLocks()).isEmpty();
        }
    }

    // 테스트를 위한 인메모리 구현체들
    static class TestLockPort implements LockPort {
        private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final Set<String> activeLocks = ConcurrentHashMap.newKeySet();

        @Override
        public void lock(String key) {
            locks.computeIfAbsent(key, k -> new ReentrantLock()).lock();
            activeLocks.add(key);
        }

        @Override
        public void unlock(String key) {
            ReentrantLock lock = locks.get(key);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                activeLocks.remove(key);
            }
        }

        public Set<String> getActiveLocks() {
            return Collections.unmodifiableSet(activeLocks);
        }
    }

    static class TestOrderRepositoryPort implements OrderRepositoryPort {
        private final List<Order> orders = new ArrayList<>();
        private final AtomicLong orderIdGenerator = new AtomicLong(1);

        @Override
        public Order saveOrder(Order order) {
            Order savedOrder = Order.create(
                    orderIdGenerator.getAndIncrement(),
                    order.getCustomerName(),
                    order.getCustomerAddress(),
                    order.getItems()
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
            return Collections.unmodifiableList(orders);
        }
    }

    static class TestProductRepositoryPort implements ProductRepositoryPort {
        private final Map<Long, Product> products = new HashMap<>();

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
            Product original = products.get(id);
            if (original == null) return null;

            // 방어적 복사 (동시성 테스트를 위해 항상 같은 인스턴스 반환)
            return Product.create(
                    original.getId(),
                    original.getName(),
                    original.getQuantity(),
                    original.getPrice(),
                    original.getCreatedAt(),
                    original.getUpdatedAt()
            );
        }

        public void saveProduct(Product product) {
            products.put(product.getId(), product);
        }
    }
}
