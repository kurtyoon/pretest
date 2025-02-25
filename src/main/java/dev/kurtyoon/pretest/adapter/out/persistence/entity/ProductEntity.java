package dev.kurtyoon.pretest.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductEntity {

    /* -------------------------------------------------- */
    /* Default Column ----------------------------------- */
    /* -------------------------------------------------- */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* -------------------------------------------------- */
    /* Information Column ------------------------------- */
    /* -------------------------------------------------- */
    @Column(name = "name")
    private String name;

    @Column(name = "quantity")
    private int quantity;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    protected ProductEntity() {}

    private ProductEntity(
            String name,
            int quantity
    ) {
        this.name = name;
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
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    /* -------------------------------------------------- */
    /* Builder ------------------------------------------ */
    /* -------------------------------------------------- */
    public static ProductEntity create(
            String name,
            int quantity
    ) {
        return new ProductEntity(name, quantity);
    }
}

