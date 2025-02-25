package dev.kurtyoon.pretest.domain.model;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final int quantity;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    public OrderItem(
            Long id,
            Long productId,
            String productName,
            int quantity
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
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

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    /* -------------------------------------------------- */
    /* Static Method ------------------------------------ */
    /* -------------------------------------------------- */
    public static OrderItem create(
            Long productId,
            String productName,
            int quantity
    ) {
        return new OrderItem(null, productId, productName, quantity);
    }
}
