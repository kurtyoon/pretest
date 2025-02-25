package dev.kurtyoon.pretest.domain;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final int price;
    private final int totalPrice;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    public OrderItem(
            Long id,
            Long productId,
            String productName,
            int quantity,
            int price
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = calculateTotalPrice();
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

    public int getPrice() {
        return price;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    /* -------------------------------------------------- */
    /* Functions ---------------------------------------- */
    /* -------------------------------------------------- */
    private int calculateTotalPrice() {
        return this.price * this.quantity;
    }

    /* -------------------------------------------------- */
    /* Static Method ------------------------------------ */
    /* -------------------------------------------------- */
    public static OrderItem create(
            Long productId,
            String productName,
            int quantity,
            int price
    ) {
        return new OrderItem(null, productId, productName, quantity, price);
    }

    public static OrderItem create(
            Long id,
            Long productId,
            String productName,
            int quantity,
            int price
    ) {
        return new OrderItem(id, productId, productName, quantity, price);
    }
}
