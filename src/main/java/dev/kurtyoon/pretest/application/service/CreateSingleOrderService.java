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
import dev.kurtyoon.pretest.domain.model.Order;
import dev.kurtyoon.pretest.domain.model.OrderItem;
import dev.kurtyoon.pretest.domain.model.Product;
import dev.kurtyoon.pretest.domain.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CreateSingleOrderService implements CreateSingleOrderUseCase {

    private final LockPort lockPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    private final OrderService orderService;

    public CreateSingleOrderService(
            LockPort lockPort,
            OrderRepositoryPort orderRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            OrderService orderService
    ) {
        this.lockPort = lockPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.orderService = orderService;
    }

    @Override
    public SingleOrderResult execute(OrderCommand command) {

        List<Long> productIdList = command.items().stream()
                .map(OrderItemCommand::productId)
                .toList();

        List<Product> productList = getValidateProducts(productIdList);

        Order order = createOrder(command);

        orderService.validateOrderWithProductList(order, productList);

        updateStock(order.getItems(), productList);

        Order savedOrder = orderRepositoryPort.saveOrder(order);

        return SingleOrderResult.of(savedOrder);
    }

    private List<Product> getValidateProducts(List<Long> productIdList) {
        List<Product> productList = productRepositoryPort.findAllByIdList(productIdList);

        if (productList.size() != productIdList.size()) {
            throw new RuntimeException("상품 정보가 일치하지 않음");
        }

        return productList;
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

    private void updateStock(
            List<OrderItem> orderItemList,
            List<Product> productList
    ) {
        List<Long> sortedProductIdList = orderItemList.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        try {
            // 상품별로 lock 설정
            sortedProductIdList.forEach(id -> lockPort.lock(getProductLockKey(id)));

            // 재고 처리
            orderItemList.forEach(orderItem -> {
                Product product = findProductById(productList, orderItem.getProductId());

                product.reduceStock(orderItem.getQuantity());
                productRepositoryPort.saveProduct(product);
            });
        } finally {
            sortedProductIdList.stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(id -> lockPort.unlock(getProductLockKey(id)));
        }
    }

    private Product findProductById(List<Product> productList, Long productId) {
        return productList.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_PRODUCT));
    }

    private String getProductLockKey(Long productId) {
        return "PRODUCT_" + productId;
    }
}
