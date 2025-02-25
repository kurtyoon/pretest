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
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import dev.kurtyoon.pretest.domain.Product;
import dev.kurtyoon.pretest.domain.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreateBulkOrderService implements CreateBulkOrderUseCase {

    private final ExcelParserPort excelParserPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    private final LockPort lockPort;
    private final OrderService orderService;

    public CreateBulkOrderService(
            ExcelParserPort excelParserPort,
            ProductRepositoryPort productRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            LockPort lockPort,
            OrderService orderService) {
        this.excelParserPort = excelParserPort;
        this.productRepositoryPort = productRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.lockPort = lockPort;
        this.orderService = orderService;
    }

    @Override
    public BulkOrderResult execute(byte[] excelData) {

        // 1. Excel 데이터 파싱
        List<OrderCommand> commandList = excelParserPort.parse(excelData);
        if (commandList.isEmpty()) {
            return BulkOrderResult.empty();
        }

        // 2. 단일 락 획득
        lockPort.lock("BULK_ORDER");

        List<Order> successOrders = new ArrayList<>();
        List<FailedOrderResult> failedOrderResults = new ArrayList<>();

        try {
            Set<Long> productIdSet = extractProductIdSet(commandList);
            List<Product> productList = productRepositoryPort.findAllByIdList(new ArrayList<>(productIdSet));
            Map<Long, Product> productMap = productList.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            for (OrderCommand command : commandList) {
                try {
                    Order order = createOrder(command);

                    // 재고 검증
                    orderService.validateOrderWithProductList(order, productList);

                    // 재고 차감
                    reduceStock(order.getItems(), productMap);

                    successOrders.add(order);
                } catch (Exception e) {
                    failedOrderResults.add(FailedOrderResult.of(
                            command.customerName(),
                            command.customerAddress(),
                            e.getMessage()
                    ));
                }
            }

            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));
            List<Order> savedOrders = orderRepositoryPort.saveAllOrder(successOrders);

            return BulkOrderResult.of(savedOrders, failedOrderResults);
        } finally {
            lockPort.unlock("BULK_ORDER");
        }
    }

    private Order createOrder(OrderCommand command) {
        List<OrderItem> orderItemList = command.items().stream()
                .map(item -> OrderItem.create(
                        item.productId(),
                        item.productName(),
                        item.quantity()
                )).toList();

        return Order.create(
                command.customerName(),
                command.customerAddress(),
                orderItemList
        );
    }

    private void reduceStock(
            List<OrderItem> orderItemList,
            Map<Long, Product> productMap
    ) {
        for (OrderItem item : orderItemList) {
            Product product = productMap.get(item.getProductId());
            product.reduceStock(item.getQuantity());
        }
    }

    private Set<Long> extractProductIdSet(List<OrderCommand> commandList) {
        return commandList.stream()
                .flatMap(c -> c.items().stream())
                .map(OrderItemCommand::productId)
                .collect(Collectors.toSet());
    }
}
