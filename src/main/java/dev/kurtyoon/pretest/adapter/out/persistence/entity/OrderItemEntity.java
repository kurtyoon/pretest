package dev.kurtyoon.pretest.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    /* -------------------------------------------------- */
    /* Default Column ----------------------------------- */
    /* -------------------------------------------------- */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* -------------------------------------------------- */
    /* Information Column ------------------------------- */
    /* -------------------------------------------------- */
    @Column(name = "quantity")
    private int quantity;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    protected OrderItemEntity() {}

    private OrderItemEntity(
        ProductEntity product,
        int quantity
    ) {
        this.product = product;
        this.quantity = quantity;
    }

    /* -------------------------------------------------- */
    /* Relation Column - Parent ------------------------- */
    /* -------------------------------------------------- */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    /* -------------------------------------------------- */
    /* Getter ------------------------------------------- */
    /* -------------------------------------------------- */
    public Long getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public ProductEntity getProduct() {
        return product;
    }

    /* -------------------------------------------------- */
    /* Update Order ------------------------------------- */
    /* -------------------------------------------------- */
    public void updateOrder(OrderEntity order) {
        this.order = order;
    }

    /* -------------------------------------------------- */
    /* Factory Method ----------------------------------- */
    /* -------------------------------------------------- */
    public static OrderItemEntity create(
            ProductEntity product,
            int quantity
    ) {
        return new OrderItemEntity(product, quantity);
    }
}
