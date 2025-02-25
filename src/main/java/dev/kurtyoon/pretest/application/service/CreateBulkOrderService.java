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
import dev.kurtyoon.pretest.domain.model.Order;
import dev.kurtyoon.pretest.domain.model.OrderItem;
import dev.kurtyoon.pretest.domain.model.Product;
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

        List<OrderCommand> commandList = excelParserPort.parse(excelData);

        if (commandList.isEmpty()) {
            return BulkOrderResult.empty();
        }

        Set<Long> productIdSet = extractProductIdSet(commandList);

        List<Long> sortedIdList = productIdSet.stream().sorted().toList();
        sortedIdList.forEach(id -> lockPort.lock(getProductLockKey(id)));

        List<Order> successOrders = new ArrayList<>();
        List<FailedOrderResult> failedOrders = new ArrayList<>();

        try {
            List<Product> productList = productRepositoryPort.findAllByIdList(sortedIdList);
            Map<Long, Product> productMap = productList.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            for (OrderCommand command : commandList) {
                try {
                    Order order = createOrder(command);

                    orderService.validateOrderWithProductList(order, new ArrayList<>());

                    reduceStock(order.getItems(), productMap);

                    successOrders.add(order);
                } catch (RuntimeException e) {
                    failedOrders.add(FailedOrderResult.of(
                            command.customerName(),
                            command.customerAddress(),
                            e.getMessage()
                    ));
                }
            }

            productRepositoryPort.saveAllProducts(new ArrayList<>(productMap.values()));
            orderRepositoryPort.saveAllOrder(successOrders);
        } finally {
            sortedIdList.stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(id -> lockPort.unlock(getProductLockKey(id)));
        }

        return BulkOrderResult.of(successOrders, failedOrders);
    }

    private Order createOrder(OrderCommand cmd) {
        List<OrderItem> orderItems = cmd.items().stream()
                .map(item -> OrderItem.create(
                        item.productId(),
                        item.productName(),
                        item.quantity()))
                .toList();

        return Order.create(cmd.customerName(), cmd.customerAddress(), orderItems);
    }

    private void reduceStock(List<OrderItem> orderItems, Map<Long, Product> productMap) {
        for (OrderItem item : orderItems) {
            Product product = productMap.get(item.getProductId());
            product.reduceStock(item.getQuantity());
        }
    }

    private Set<Long> extractProductIdSet(List<OrderCommand> commands) {
        return commands.stream()
                .flatMap(c -> c.items().stream())
                .map(OrderItemCommand::productId)
                .collect(Collectors.toSet());
    }

    private String getProductLockKey(Long productId) {
        return "PRODUCT_LOCK_" + productId;
    }
}
