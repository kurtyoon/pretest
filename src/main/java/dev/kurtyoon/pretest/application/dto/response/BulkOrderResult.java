package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.Order;

import java.util.List;

public class BulkOrderResult extends SelfValidating<BulkOrderResult> {

    @JsonProperty("total_orders")
    private final int totalOrders;

    @JsonProperty("success_orders")
    private final List<SingleOrderResult> successOrders;

    @JsonProperty("failed_orders")
    private final List<FailedOrderResult> failedOrders;

    public BulkOrderResult(
            int totalOrders,
            List<SingleOrderResult> successOrders,
            List<FailedOrderResult> failedOrders
    ) {
        this.totalOrders = totalOrders;
        this.successOrders = successOrders;
        this.failedOrders = failedOrders;

        this.validateSelf();
    }

    public static BulkOrderResult of(List<Order> successOrders, List<FailedOrderResult> failedOrders) {

        List<SingleOrderResult> successOrderResults = successOrders.stream()
                .map(SingleOrderResult::of)
                .toList();

        return new BulkOrderResult(
                successOrders.size() + failedOrders.size(),
                successOrderResults,
                failedOrders
        );
    }

    public static BulkOrderResult empty() {
        return new BulkOrderResult(0, List.of(), List.of());
    }
}
