package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.model.Order;
import dev.kurtyoon.pretest.domain.model.OrderItem;

import java.util.List;

public class SingleOrderResult extends SelfValidating<SingleOrderResult> {

    @JsonProperty("order_id")
    private final Long orderId;

    @JsonProperty("customer_name")
    private final String customerName;

    @JsonProperty("customer_address")
    private final String customerAddress;

    @JsonProperty("products")
    private final List<OrderItemResult> products;

    public SingleOrderResult(
            Long orderId,
            String customerName,
            String customerAddress,
            List<OrderItemResult> products
    ) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerAddress = customerAddress;

        this.products = products;

        this.validateSelf();
    }

    public static SingleOrderResult of(Order order) {

        List<OrderItemResult> orderItemResults = order.getItems().stream()
                .map(OrderItemResult::of)
                .toList();

        return new SingleOrderResult(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerAddress(),
                orderItemResults
        );
    }
}
