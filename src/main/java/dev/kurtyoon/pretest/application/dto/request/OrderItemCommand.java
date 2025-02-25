package dev.kurtyoon.pretest.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemCommand(

        @JsonProperty("product_id")
        @NotNull
        Long productId,

        @JsonProperty("product_name")
        @NotNull
        String productName,

        @JsonProperty("quantity")
        @Min(1)
        int quantity
) {
}
