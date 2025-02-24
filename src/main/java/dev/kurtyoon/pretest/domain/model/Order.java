package dev.kurtyoon.pretest.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Order {

    private final Long id;
    private final String customerName;
    private final String customerAddress;
    private final List<OrderItem> items = new ArrayList<>();

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    private Order(
            Long id,
            String customerName,
            String customerAddress,
            List<OrderItem> items
    ) {
        this.id = id;
        this.customerName = customerName;
        this.customerAddress = customerAddress;

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
}
