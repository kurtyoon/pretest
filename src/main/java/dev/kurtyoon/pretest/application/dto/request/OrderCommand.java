package dev.kurtyoon.pretest.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCommand(

        @JsonProperty("customer_name")
        @NotNull
        String customerName,

        @JsonProperty("customer_address")
        @NotNull
        String customerAddress,

        @JsonProperty("items")
        @NotEmpty
        List<OrderItemCommand> items
) {
}
