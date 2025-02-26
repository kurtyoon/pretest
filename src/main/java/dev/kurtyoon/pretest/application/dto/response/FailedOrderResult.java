package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import jakarta.validation.constraints.NotBlank;

public class FailedOrderResult extends SelfValidating<FailedOrderResult> {

    @JsonProperty("customer_name")
    @NotBlank(message = "고객 이름은 필수 입력값입니다.")
    private final String customerName;

    @JsonProperty("customer_address")
    @NotBlank(message = "고객 주소는 필수 입력값입니다.")
    private final String customerAddress;

    @JsonProperty("reason")
    @NotBlank(message = "실패 사유는 필수 입력값입니다.")
    private final String reason;

    public String getCustomerName() {
        return customerName;
    }

    public String getReason() {
        return reason;
    }

    public FailedOrderResult(
            String customerName,
            String customerAddress,
            String reason
    ) {
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.reason = reason;

        this.validateSelf();
    }

    public static FailedOrderResult of(
            String customerName,
            String customerAddress,
            String reason
    ) {
        return new FailedOrderResult(customerName, customerAddress, reason);
    }
}
