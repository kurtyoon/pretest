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
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import dev.kurtyoon.pretest.domain.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreateBulkOrderService implements CreateBulkOrderUseCase {

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

        // 2. 전체 주문에 대한 Lock 획득
        lockPort.lock("BULK_ORDER");

        List<Order> successOrders = new ArrayList<>();
        List<FailedOrderResult> failedOrderResults = new ArrayList<>();

        try {
            // 3. 전체 상품 목록 조회 및 Map 변환 (한 번만 호출하여 성능 최적화)
            Map<Long, Product> productMap = getProductMap(commandList);

            // 4. 주문 처리
            for (OrderCommand command : commandList) {
                try {
                    // (1) 주문 생성
                    Order order = createOrder(command, productMap);

                    // (2) 주문 검증 (중복된 상품 여부 체크)
                    validateDuplicateProductInOrder(order);

                    // (3) 재고 차감
                    reduceStock(order.getItems(), productMap);

                    successOrders.add(order);
                } catch (CommonException e) {
                    failedOrderResults.add(FailedOrderResult.of(
                            command.customerName(),
                            command.customerAddress(),
                            e.getMessage()
                    ));
                }
            }

            // 5. 모든 상품의 변경된 재고 한 번에 저장 (Batch 처리)
            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));

            // 6. 모든 주문 한 번에 저장 (Batch 처리)
            List<Order> savedOrders = orderRepositoryPort.saveAllOrder(successOrders);

            return BulkOrderResult.of(savedOrders, failedOrderResults);
        } finally {
            // 7. Lock 해제
            lockPort.unlock("BULK_ORDER");
        }
    }

    /**
     * 주문 생성 (상품이 존재하는지 검증 포함)
     */
    private Order createOrder(OrderCommand command, Map<Long, Product> productMap) {
        List<OrderItem> orderItemList = command.items().stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    if (product == null) {
                        throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
                    }
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

    /**
     * 주문 내 중복 상품 검증
     */
    private void validateDuplicateProductInOrder(Order order) {
        Set<Long> productIdSet = new HashSet<>();
        for (OrderItem item : order.getItems()) {
            if (!productIdSet.add(item.getProductId())) {
                throw new CommonException(ErrorCode.DUPLICATE_PRODUCT_ORDER);
            }
        }
    }

    /**
     * 전체 주문에서 필요한 상품 목록을 조회하고 Map으로 변환
     */
    private Map<Long, Product> getProductMap(List<OrderCommand> commandList) {
        Set<Long> productIdSet = extractProductIdSet(commandList);
        List<Product> productList = productRepositoryPort.findAllByIdList(new ArrayList<>(productIdSet));
        return productList.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    /**
     * 주문 내 모든 상품의 재고 차감
     */
    private void reduceStock(List<OrderItem> orderItemList, Map<Long, Product> productMap) {
        for (OrderItem item : orderItemList) {
            Product product = productMap.get(item.getProductId());

            if (product.getQuantity() < item.getQuantity()) {
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }

            product.reduceStock(item.getQuantity());
        }
    }

    /**
     * 모든 주문에서 상품 ID 추출 (중복 제거)
     */
    private Set<Long> extractProductIdSet(List<OrderCommand> commandList) {
        return commandList.stream()
                .flatMap(c -> c.items().stream())
                .map(OrderItemCommand::productId)
                .collect(Collectors.toSet());
    }
}
