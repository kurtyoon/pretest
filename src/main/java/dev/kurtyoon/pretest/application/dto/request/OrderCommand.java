package dev.kurtyoon.pretest.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record OrderCommand(

        @JsonProperty("customer_name")
        @NotBlank(message = "고객 이름은 필수입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s]*$", message = "고객 일므에 특수문자를 사용할 수 없습니다.")
        String customerName,

        @JsonProperty("customer_address")
        @NotBlank(message = "배송 주소는 필수입니다.")
        String customerAddress,

        @JsonProperty("items")
        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemCommand> items
) {
}
