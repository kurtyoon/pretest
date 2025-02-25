package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BulkOrderResult extends SelfValidating<BulkOrderResult> {

    @JsonProperty("total_orders")
    @Min(value = 0, message = "총 주문 개수는 0개 이상이어야 합니다.")
    private final int totalOrders;

    @JsonProperty("success_orders")
    @NotNull(message = "성공한 주문 목록은 필수 입력값입니다.")
    @Valid
    private final List<SingleOrderResult> successOrders;

    @JsonProperty("failed_orders")
    @NotNull(message = "실패한 주문 목록은 필수 입력값입니다.")
    @Valid
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
