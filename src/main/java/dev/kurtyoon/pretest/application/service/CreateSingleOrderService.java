package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateSingleOrderUseCase;
import dev.kurtyoon.pretest.application.port.out.LockPort;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
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

        // 1. 상품 ID 목록 추출 및 정렬
        List<Long> sortedProductIdList = getSortedProductIdList(command);

        // 2. 상품 중복 체크
        validateNoDuplicateProducts(sortedProductIdList);

        // 3. Lock 획득
        for (Long productId : sortedProductIdList) {
            lockPort.lock(getProductLockKey(productId));
        }

        try {
            // 4. 한 번의 DB 조회로 모든 상품 가져오기
            Map<Long, Product> productMap = getProductMap(sortedProductIdList);

            // 5. 주문 생성
            Order order = createOrderWithItems(command, productMap);

            // 6. 재고 확인 및 차감
            updateProductStock(order.getItems(), productMap);

            // 7. 주문 저장 및 변경된 상품 정보 저장
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));
            Order savedOrder = orderRepositoryPort.saveOrder(order);

            return SingleOrderResult.of(savedOrder);
        } finally {
            // 8. 역순으로 Lock 해제
            releaseAllLocks(sortedProductIdList);
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

    private void updateProductStock(
            List<OrderItem> orderItemList,
            Map<Long, Product> productMap
    ) {
        for (OrderItem item : orderItemList) {
            Product product = productMap.get(item.getProductId());

            if (product.getQuantity() < item.getQuantity()) {
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }

            product.reduceStock(item.getQuantity());
        }
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
