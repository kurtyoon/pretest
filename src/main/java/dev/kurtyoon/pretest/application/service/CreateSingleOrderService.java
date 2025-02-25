package dev.kurtyoon.pretest.application.service;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateSingleOrderUseCase;
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
public class CreateSingleOrderService implements CreateSingleOrderUseCase {

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

        String orderLockKey = getOrderLockKey(command.customerAddress());
        lockPort.lock(orderLockKey);

        try {
            List<Long> productIdList = command.items().stream()
                    .map(OrderItemCommand::productId)
                    .toList();

            // 중복된 상품이 존재하는지 검증
            validateDuplicate(productIdList);

            List<Product> productList = productRepositoryPort.findAllByIdList(productIdList);
            Map<Long, Product> productMap = productList.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            Order order = createOrder(command, productMap);

            updateStock(order.getItems(), productMap);

            Order savedOrder = orderRepositoryPort.saveOrder(order);

            return SingleOrderResult.of(savedOrder);

        } finally {
            lockPort.unlock(orderLockKey);
        }
    }

    private Order createOrder(
            OrderCommand command,
            Map<Long, Product> productMap
    ) {
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

                }).toList();

        return Order.create(
                command.customerName(),
                command.customerAddress(),
                orderItemList
        );
    }

    private void updateStock(
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

        productRepositoryPort.saveAllProducts(productMap.values().stream().toList());
    }

    private void validateDuplicate(List<Long> productIdList) {
        Set<Long> distinct = new HashSet<>(productIdList);

        if (distinct.size() != productIdList.size()) {
            throw new CommonException(ErrorCode.DUPLICATE_PRODUCT_ORDER);
        }
    }

    private String getOrderLockKey(String key) {
        return "ORDER_LOCK" + key;
    }
}
