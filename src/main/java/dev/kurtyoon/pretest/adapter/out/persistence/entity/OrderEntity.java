package dev.kurtyoon.pretest.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
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

    @Column(name = "total_price")
    private Integer totalPrice;

    @Column(name =  "ordered_at")
    private LocalDateTime orderedAt;

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
            String customerAddress,
            Integer totalPrice,
            LocalDateTime orderedAt
    ) {
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.totalPrice = totalPrice;
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

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
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
            Integer totalPrice,
            LocalDateTime orderedAt,
            List<OrderItemEntity> items
    ) {
        OrderEntity order = new OrderEntity(customerName, customerAddress, totalPrice, orderedAt);

        for (OrderItemEntity item: items) {
            order.addOrderItem(item);
        }

        return order;
    }
}
