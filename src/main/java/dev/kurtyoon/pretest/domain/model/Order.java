package dev.kurtyoon.pretest.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private final Long id;
    private final String customerName;
    private final String customerAddress;
    private final List<OrderItem> items = new ArrayList<>();
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
        this.id = id;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.orderedAt = orderedAt;

        if (items != null) {
            this.items.addAll(items);
        }
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

    /* -------------------------------------------------- */
    /* Static Method ------------------------------------ */
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
