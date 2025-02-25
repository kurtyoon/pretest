package dev.kurtyoon.pretest.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.common.date.DateUtils;
import dev.kurtyoon.pretest.core.annotation.DateTimeValue;
import dev.kurtyoon.pretest.core.dto.SelfValidating;
import dev.kurtyoon.pretest.domain.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class SingleOrderResult extends SelfValidating<SingleOrderResult> {

    @JsonProperty("order_id")
    @NotNull(message = "주문 ID는 필수 입력값입니다.")
    private final Long orderId;

    @JsonProperty("customer_name")
    @NotBlank(message = "주문자 이름은 필수 입력값입니다.")
    private final String customerName;

    @JsonProperty("customer_address")
    @NotBlank(message = "주문자 주소는 필수 입력값입니다.")
    private final String customerAddress;

    @JsonProperty("total_price")
    @Positive(message = "총 가격은 0원 이상이어야 합니다.")
    private final int totalPrice;

    @JsonProperty("ordered_at")
    @DateTimeValue
    @NotBlank(message = "주문 날짜는 필수 입력값입니다.")
    private final String orderedAt;

    @JsonProperty("products")
    @NotNull(message = "상품 목록은 필수 입력값입니다.")
    private final List<OrderItemResult> products;

    public SingleOrderResult(
            Long orderId,
            String customerName,
            String customerAddress,
            int totalPrice,
            String orderedAt,
            List<OrderItemResult> products
    ) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.totalPrice = totalPrice;
        this.orderedAt = orderedAt;
        this.products = products;

        this.validateSelf();
    }

    public static SingleOrderResult of(Order order) {

        List<OrderItemResult> orderItemResults = order.getItems().stream()
                .map(OrderItemResult::of)
                .toList();

        return new SingleOrderResult(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getTotalPrice(),
                DateUtils.convertLocalDateTimeToString(order.getOrderedAt()),
                orderItemResults
        );
    }
}
