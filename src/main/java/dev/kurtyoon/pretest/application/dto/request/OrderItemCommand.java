package dev.kurtyoon.pretest.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemCommand(

        @JsonProperty("product_id")
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @JsonProperty("product_name")
        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @JsonProperty("quantity")
        @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다.")
        int quantity
) {
}
