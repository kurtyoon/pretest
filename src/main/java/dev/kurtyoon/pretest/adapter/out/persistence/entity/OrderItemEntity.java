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
    private Integer quantity;

    @Column(name = "price")
    private Integer price;

    @Column(name = "total_price")
    private Integer totalPrice;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    protected OrderItemEntity() {}

    private OrderItemEntity(
        ProductEntity product,
        Integer quantity,
        Integer price,
        Integer totalPrice
    ) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
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

    public Integer getPrice() {
        return price;
    }

    public Integer getTotalPrice() {
        return totalPrice;
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
            Integer quantity,
            Integer price,
            Integer totalPrice
    ) {
        return new OrderItemEntity(product, quantity, price, totalPrice);
    }
}
