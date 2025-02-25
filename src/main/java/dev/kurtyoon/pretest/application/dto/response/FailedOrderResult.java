package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;

public class FailedOrderResult extends SelfValidating<FailedOrderResult> {

    @JsonProperty("customer_name")
    private final String customerName;

    @JsonProperty("customer_address")
    private final String customerAddress;

    @JsonProperty("reason")
    private final String reason;

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
