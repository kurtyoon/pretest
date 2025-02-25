package dev.kurtyoon.pretest.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
    /* -------------------------------------------------- */
    /* Default Column ----------------------------------- */
    /* -------------------------------------------------- */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* -------------------------------------------------- */
    /* Information Column ------------------------------- */
    /* -------------------------------------------------- */
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_address")
    private String customerAddress;

    /* -------------------------------------------------- */
    /* Relation Column - Child -------------------------- */
    /* -------------------------------------------------- */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    protected OrderEntity() {}

    private OrderEntity(
            String customerName,
            String customerAddress
    ) {
        this.customerName = customerName;
        this.customerAddress = customerAddress;
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

    public List<OrderItemEntity> getOrderItems() {
        return orderItems;
    }

    /* -------------------------------------------------- */
    /* Functions ---------------------------------------- */
    /* -------------------------------------------------- */
    public void addOrderItem(OrderItemEntity orderItem) {
        orderItems.add(orderItem);
        orderItem.updateOrder(this);
    }

    /* -------------------------------------------------- */
    /* Factory Method ----------------------------------- */
    /* -------------------------------------------------- */
    public static OrderEntity create(
            String customerName,
            String customerAddress,
            List<OrderItemEntity> items
    ) {
        OrderEntity order = new OrderEntity(customerName, customerAddress);

        for (OrderItemEntity item: items) {
            order.addOrderItem(item);
        }

        return order;
    }
}
