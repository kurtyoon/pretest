package dev.kurtyoon.pretest.domain.model;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final int quantity;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    public OrderItem(
            Long id,
            Long productId,
            int quantity
    ) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    /* -------------------------------------------------- */
    /* Getter ------------------------------------------- */
    /* -------------------------------------------------- */
    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    /* -------------------------------------------------- */
    /* Static Method ------------------------------------ */
    /* -------------------------------------------------- */
    public static OrderItem create(
            Long productId,
            int quantity
    ) {
        return new OrderItem(null, productId, quantity);
    }
}
