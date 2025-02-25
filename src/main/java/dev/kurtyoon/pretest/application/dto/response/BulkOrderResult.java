package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.domain.model.Order;
import dev.kurtyoon.pretest.domain.model.OrderItem;

import java.util.List;

public class BulkOrderResult {

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
    }

    public static BulkOrderResult of(List<Order> successOrders, List<FailedOrderResult> failedOrders) {

        List<SingleOrderResult> successOrderResults = successOrders.stream()
                .map(order -> SingleOrderResult.of(order, order.getItems()))
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
