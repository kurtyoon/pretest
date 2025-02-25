package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.OrderItem;

public class OrderItemResult extends SelfValidating<OrderItemResult> {

    @JsonProperty("product_id")
    private final Long productId;

    @JsonProperty("product_name")
    private final String productName;

    @JsonProperty("quantity")
    private final int quantity;

    public OrderItemResult(
            Long productId,
            String productName,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;

        this.validateSelf();
    }

    public static OrderItemResult of(OrderItem orderItem) {
        return new OrderItemResult(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity()
        );
    }
}
