package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;
import dev.kurtyoon.pretest.application.dto.response.FailedOrderResult;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateBulkOrderUseCase;
import dev.kurtyoon.pretest.application.port.out.ExcelParserPort;
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
public class CreateBulkOrderService implements CreateBulkOrderUseCase {

    private final static Logger log = LoggerUtils.getLogger(CreateBulkOrderService.class);

    private final ExcelParserPort excelParserPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    private final LockPort lockPort;

    public CreateBulkOrderService(
            ExcelParserPort excelParserPort,
            ProductRepositoryPort productRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            LockPort lockPort
    ) {
        this.excelParserPort = excelParserPort;

        this.productRepositoryPort = productRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.lockPort = lockPort;
    }

    @Override
    public BulkOrderResult execute(byte[] excelData) {
        // 1. Excel 데이터 파싱
        List<OrderCommand> commandList = excelParserPort.parse(excelData);
        if (commandList.isEmpty()) {
            return BulkOrderResult.empty();
        }

        // 2. 각 주문 내 중복 상품 검증 (사전 검증으로 불필요한 락 획득 방지)
        validateNoDuplicateInEachOrder(commandList);

        // 3. 필요한 모든 상품 ID 추출 및 정렬
        List<Long> sortedProductIds = getSortedUniqueProductIds(commandList);

        try {
            // 4. 오름차순으로 락 획득 (데드락 방지)
            acquireProductLocks(sortedProductIds);

            // 5. 주문 처리
            return processBulkOrder(commandList, sortedProductIds);
        } finally {
            // 6. 역순으로 락 해제 (데드락 방지)
            releaseProductLocks(sortedProductIds);
        }
    }

    private void validateNoDuplicateInEachOrder(List<OrderCommand> commandList) {
        for (OrderCommand command : commandList) {
            Set<Long> productIds = new HashSet<>();
            for (OrderItemCommand item : command.items()) {
                if (!productIds.add(item.productId())) {
                    throw new CommonException(ErrorCode.DUPLICATE_PRODUCT_ORDER);
                }
            }
        }
    }

    private List<Long> getSortedUniqueProductIds(List<OrderCommand> commandList) {

        return commandList.stream()
                .flatMap(c -> c.items().stream())
                .map(OrderItemCommand::productId).distinct().sorted().collect(Collectors.toList());
    }

    private void acquireProductLocks(List<Long> productIds) {
        for (Long productId : productIds) {
            lockPort.lock(getProductLockKey(productId));
            log.debug("Lock acquired for product: {}", productId);
        }
    }

    private void releaseProductLocks(List<Long> productIds) {
        for (int i = productIds.size() - 1; i >= 0; i--) {
            Long productId = productIds.get(i);
            try {
                lockPort.unlock(getProductLockKey(productId));
                log.debug("Lock released for product: {}", productId);
            } catch (Exception e) {
                log.error("Failed to release lock for product {}: {}", productId, e.getMessage());
                // 락 해제 실패는 로깅만 하고 계속 진행 (다른 락들은 해제해야 함)
            }
        }
    }

    private BulkOrderResult processBulkOrder(
            List<OrderCommand> commandList,
            List<Long> productIds
    ) {
        List<Order> successOrders = new ArrayList<>();
        List<FailedOrderResult> failedOrderResults = new ArrayList<>();

        // 1. 전체 상품 조회 (한 번만 DB 접근)
        Map<Long, Product> productMap = fetchAndValidateProducts(productIds);

        // 2. 전체 주문량 계산 및 재고 사전 검증 (전체 주문이 실패할 경우 빠른 실패)
        validateSufficientStockForAllOrders(commandList, productMap);

        // 3. 각 주문 처리
        for (OrderCommand command : commandList) {
            try {
                // 주문 생성
                Order order = createOrder(command, productMap);

                // 재고 차감
                reduceStock(order.getItems(), productMap);

                successOrders.add(order);
            } catch (CommonException e) {
                log.debug("Order failed for customer {}: {}", command.customerName(), e.getMessage());
                failedOrderResults.add(FailedOrderResult.of(
                        command.customerName(),
                        command.customerAddress(),
                        e.getMessage()
                ));
            }
        }

        // 4. 데이터 저장 (배치 처리)
        if (!successOrders.isEmpty()) {
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));
            List<Order> savedOrders = orderRepositoryPort.saveAllOrder(successOrders);
            log.info("Processed {} successful orders out of {} total orders",
                    successOrders.size(), commandList.size());
            return BulkOrderResult.of(savedOrders, failedOrderResults);
        } else {
            log.info("No successful orders out of {} total orders", commandList.size());
            return BulkOrderResult.of(List.of(), failedOrderResults);
        }
    }

    private Map<Long, Product> fetchAndValidateProducts(List<Long> productIds) {
        List<Product> products = productRepositoryPort.findAllByIdList(productIds);

        if (products.size() != productIds.size()) {
            Set<Long> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            List<Long> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            log.error("Missing products: {}", missingIds);
            throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private void validateSufficientStockForAllOrders(
            List<OrderCommand> commandList,
            Map<Long, Product> productMap
    ) {
        Map<Long, Integer> totalRequiredQuantities = new HashMap<>();

        // 상품별 총 필요 수량 계산
        for (OrderCommand command : commandList) {
            for (OrderItemCommand item : command.items()) {
                totalRequiredQuantities.merge(item.productId(), item.quantity(), Integer::sum);
            }
        }

        // 재고가 충분한지 검증
        for (Map.Entry<Long, Integer> entry : totalRequiredQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();
            Product product = productMap.get(productId);

            if (product.getQuantity() < requiredQuantity) {
                log.debug("Insufficient stock for product {}: required={}, available={}",
                        productId, requiredQuantity, product.getQuantity());
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }
        }

        log.debug("Sufficient stock validated for all products");
    }

    private Order createOrder(
            OrderCommand command,
            Map<Long, Product> productMap
    ) {
        List<OrderItem> orderItems = command.items().stream()
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
                orderItems
        );
    }

    private void reduceStock(
            List<OrderItem> orderItems,
            Map<Long, Product> productMap
    ) {
        for (OrderItem item : orderItems) {
            Product product = productMap.get(item.getProductId());

            if (product.getQuantity() < item.getQuantity()) {
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }

            product.reduceStock(item.getQuantity());
        }
    }

    private String getProductLockKey(Long productId) {
        return String.format("PRODUCT_LOCK:%d", productId);
    }
}
