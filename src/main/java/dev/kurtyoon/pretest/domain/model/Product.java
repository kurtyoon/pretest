package dev.kurtyoon.pretest.domain.model;

public class Product {

    private final Long id;
    private final String name;
    private int quantity;


    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    public Product(
            Long id,
            String name,
            int quantity
    ) {
        this.id = id;
        this.name = (name != null) ? name : "";
        this.quantity = quantity;
    }

    /* -------------------------------------------------- */
    /* Getter ------------------------------------------- */
    /* -------------------------------------------------- */
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    /* -------------------------------------------------- */
    /* Functions ---------------------------------------- */
    /* -------------------------------------------------- */
    public void reduceStock(int quantity) {
        this.quantity -= quantity;
    }
}
