package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.domain.model.OrderItem;

public class OrderItemResult {

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
    }

    public static OrderItemResult of(OrderItem orderItem) {
        return new OrderItemResult(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity()
        );
    }
}
