package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateSingleOrderUseCase;
import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.application.service.support.OrderExecutionContext;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import dev.kurtyoon.pretest.domain.Product;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreateSingleOrderService implements CreateSingleOrderUseCase {

    private final static Logger log = LoggerUtils.getLogger(CreateSingleOrderService.class);

    private final LockPort lockPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    public CreateSingleOrderService(
            LockPort lockPort,
            OrderRepositoryPort orderRepositoryPort,
            ProductRepositoryPort productRepositoryPort
    ) {
        this.lockPort = lockPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
    }

    @Override
    public SingleOrderResult execute(OrderCommand command) {

        // 0. 주문 요청에 대한 유효성 검증
        if (command.items() == null || command.items().isEmpty()) {
            throw new CommonException(ErrorCode.INVALID_ORDER);
        }

        // 1. 상품 ID 목록 추출 및 정렬
        List<Long> sortedProductIdList = getSortedProductIdList(command);

        // 2. 상품 중복 체크
        validateNoDuplicateProducts(sortedProductIdList);

        // 3. 실행 컨텍스트 (Lock 관리, 재고 상태 추적)
        OrderExecutionContext context = new OrderExecutionContext(sortedProductIdList);

        try {
            // 4. Lock 획득
            acquireAllLocks(context);

            // 5. 상품 조회 및 주문 처리
            return processOrder(command, context);
        } catch (Exception e) {
            log.error("Failed to Order: {}", e.getMessage());
            throw e;
        } finally {
            // 획득한 Lock 해제
            releaseAllLocks(context.getAcquiredLockList());
        }
    }

    private SingleOrderResult processOrder(
            OrderCommand command,
            OrderExecutionContext context
    ) {
        // 1. 상품 조회
        Map<Long, Product> productMap = getProductMap(context.getProductIdList());

        // 2. 재고 상태 백업
        context.backupStockState(productMap);

        try {
            // 3. 주문 생성
            Order order = createOrderWithItems(command, productMap);

            // 4. 재고 확인 및 차감
            if (!context.validateAndReduceStock(order.getItems(), productMap)) {
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }

            // 5. 변경된 상품 정보 저장
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));

            // 6. 주문 저장
            Order savedOrder = orderRepositoryPort.saveOrder(order);

            log.info("주문 처리 성공: 고객 = {}, 주문 번호 = {}, 상품 개수 = {}",
                    savedOrder.getCustomerName(), savedOrder.getId(), savedOrder.getItems().size());

            return SingleOrderResult.of(savedOrder);
        } catch (Exception e) {
            // 재고 복구
            context.restoreState(productMap);

            // 복구 상태 반영
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));

            throw e;
        }
    }

    private void acquireAllLocks(OrderExecutionContext context) {
        for (Long productId : context.getProductIdList()) {
            String lockKey = getProductLockKey(productId);
            lockPort.lock(lockKey);
            context.lockAcquired(productId);
            log.debug("Lock acquired for Product: {}", productId);
        }
    }

    private List<Long> getSortedProductIdList(OrderCommand command) {
        return command.items().stream()
                .map(OrderItemCommand::productId)
                .sorted()
                .toList();
    }

    private void validateNoDuplicateProducts(List<Long> productIdList) {
        Set<Long> uniqueIdSet = new HashSet<>(productIdList);
        if (uniqueIdSet.size() != productIdList.size()) {
            throw new CommonException(ErrorCode.DUPLICATE_PRODUCT_ORDER);
        }
    }

    private Map<Long, Product> getProductMap(List<Long> productIdList) {
        List<Product> productList = productRepositoryPort.findAllByIdList(productIdList);

        // 모든 상품이 존재하는지 확인
        if (productList.size() != productIdList.size()) {
            throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        return productList.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private Order createOrderWithItems(
            OrderCommand command,
            Map<Long, Product> productMap
    ) {
        List<OrderItem> orderItemList = command.items().stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    return OrderItem.create(
                            item.productId(),
                            product.getName(),
                            item.quantity(),
                            product.getPrice()
                    );
                })
                .toList();

        return Order.create(
                command.customerName(),
                command.customerAddress(),
                orderItemList
        );
    }

    private void releaseAllLocks(List<Long> productIdList) {

        // 역순으로 락 해제
        for (int i = productIdList.size() - 1; i >= 0; i--) {
            try {
                lockPort.unlock(getProductLockKey(productIdList.get(i)));
            } catch (Exception e) {
                log.error("Failed to release lock for product {}: {}",
                        productIdList.get(i), e.getMessage());
                // 락 해제 실패는 로깅 후 진행 (다른 락은 해제 시도)
            }
        }
    }

    private String getProductLockKey(Long productId) {
        return String.format("PRODUCT_LOCK:%d", productId);
    }
}
