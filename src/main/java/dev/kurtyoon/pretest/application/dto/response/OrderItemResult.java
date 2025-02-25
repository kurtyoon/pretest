package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.OrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class OrderItemResult extends SelfValidating<OrderItemResult> {

    @JsonProperty("product_id")
    @NotNull(message = "상품 ID는 필수 입력값입니다.")
    private final Long productId;

    @JsonProperty("product_name")
    @NotBlank(message = "상품 이름은 필수 입력값입니다.")
    private final String productName;

    @JsonProperty("quantity")
    @Positive(message = "수량은 1개 이상이어야 합니다.")
    private final int quantity;

    @JsonProperty("price")
    @PositiveOrZero(message = "상품 가격은 0원 이상이어야 합니다.")
    private final int price;

    @JsonProperty("total_price")
    @PositiveOrZero(message = "총 가격은 0원 이상이어야 합니다.")
    private final int totalPrice;

    public OrderItemResult(
            Long productId,
            String productName,
            int quantity,
            int price,
            int totalPrice
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;

        this.validateSelf();
    }

    public static OrderItemResult of(OrderItem orderItem) {
        return new OrderItemResult(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getTotalPrice()
        );
    }
}
