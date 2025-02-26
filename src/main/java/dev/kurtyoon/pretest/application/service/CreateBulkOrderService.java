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

        // 4. 주문 컨텍스트 생성
        OrderExecutionContext context = new OrderExecutionContext(sortedProductIds);

        try {
            // 5. Lock 획득
            acquireAllLocks(context);

            // 5. 주문 처리
            return processBulkOrder(commandList, context);
        } finally {
            // 7. Lock 해제
            releaseAllLocks(sortedProductIds);
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

    private void acquireAllLocks(OrderExecutionContext context) {
        for (Long productId : context.getProductIdList()) {
            lockPort.lock(getProductLockKey(productId));
            context.lockAcquired(productId);
            log.debug("Lock acquired for product: {}", productId);
        }
    }

    private void releaseAllLocks(List<Long> productIdList) {
        for (int i = productIdList.size() - 1; i >= 0; i--) {
            Long productId = productIdList.get(i);
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
            OrderExecutionContext context
    ) {
        List<Order> successOrders = new ArrayList<>();
        List<FailedOrderResult> failedOrderResults = new ArrayList<>();

        // 1. 전체 상품 조회 (한 번만 DB 접근)
        Map<Long, Product> productMap = fetchProducts(context.getProductIdList());

        // 2. 재고 상태 백업
        context.backupStockState(productMap);

        try {

            // 4. 각 주문을 독립적으로 처리
            for (OrderCommand command : commandList) {
                try {

                    validateProductExists(command, productMap);

                    // 주문 생성
                    Order order = createOrder(command, productMap);

                    // 재고 차감
                    if (!context.validateAndReduceStock(order.getItems(), productMap)) {
                        throw new CommonException(ErrorCode.OUT_OF_STOCK);
                    }

                    successOrders.add(order);
                } catch (CommonException e) {
                    log.debug("Order failed for Customer {}: {}", command.customerName(), e.getMessage());
                    failedOrderResults.add(FailedOrderResult.of(
                            command.customerName(),
                            command.customerAddress(),
                            e.getMessage()
                    ));
                }
            }

            // 5. 데이터 저장
            if (!successOrders.isEmpty()) {
                productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));

                List<Order> savedOrders = orderRepositoryPort.saveAllOrder(successOrders);

                log.info("Processed {} successful orders out of {} total orders", successOrders.size(), commandList.size());

                return BulkOrderResult.of(savedOrders, failedOrderResults);
            } else {
                log.info("No successful order out of {} total orders", commandList.size());
                return BulkOrderResult.of(List.of(), failedOrderResults);
            }
        } catch (Exception e) {
            // 재고 복구
            context.restoreState(productMap);

            // 재고 저장
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));

            throw e;
        }
    }

    private Map<Long, Product> fetchProducts(List<Long> productIds) {
        List<Product> products = productRepositoryPort.findAllByIdList(productIds);
        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private void validateProductExists(
            OrderCommand command,
            Map<Long, Product> productMap
    ) {
        for (OrderItemCommand item: command.items()) {
            if (!productMap.containsKey(item.productId())) {
                log.error("Product not found: {}", item.productId());
                throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
            }
        }
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

    private String getProductLockKey(Long productId) {
        return String.format("PRODUCT_LOCK:%d", productId);
    }
}
