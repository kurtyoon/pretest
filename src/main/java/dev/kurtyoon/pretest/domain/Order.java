package dev.kurtyoon.pretest.domain;

import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class Order {

    private final Long id;
    private final String customerName;
    private final String customerAddress;
    private final List<OrderItem> items;
    private final int totalPrice;
    private final LocalDateTime orderedAt;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    private Order(
            Long id,
            String customerName,
            String customerAddress,
            List<OrderItem> items,
            LocalDateTime orderedAt
    ) {

        if (items == null || items.isEmpty()) {
            throw new CommonException(ErrorCode.INVALID_ORDER);
        }

        this.id = id;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.items = Collections.unmodifiableList(items);
        this.totalPrice = calculateTotalPrice(items);
        this.orderedAt = orderedAt;
    }

    /* -------------------------------------------------- */
    /* Getter ------------------------------------------- */
    /* -------------------------------------------------- */
    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    /* -------------------------------------------------- */
    /* Functions ---------------------------------------- */
    /* -------------------------------------------------- */
    private int calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }

    /* -------------------------------------------------- */
    /* Static Factory Method ---------------------------- */
    /* -------------------------------------------------- */
    public static Order create(
            String customerName,
            String customerAddress,
            List<OrderItem> items
    ) {
        return new Order(null, customerName, customerAddress, items, LocalDateTime.now());
    }

    public static Order create(
            Long id,
            String customerName,
            String customerAddress,
            List<OrderItem> items
    ) {
        return new Order(id, customerName, customerAddress, items, LocalDateTime.now());
    }
}
