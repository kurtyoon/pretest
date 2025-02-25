package dev.kurtyoon.pretest.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
    private Integer quantity;

    @Column(name = "price")
    private Integer price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    protected ProductEntity() {}

    private ProductEntity(
            String name,
            Integer quantity,
            Integer price,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.name = name;
        this.quantity = quantity;

        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPrice() {
        return price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /* -------------------------------------------------- */
    /* Functions ---------------------------------------- */
    /* -------------------------------------------------- */
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /* -------------------------------------------------- */
    /* Builder ------------------------------------------ */
    /* -------------------------------------------------- */
    public static ProductEntity create(
            String name,
            Integer quantity,
            Integer price,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new ProductEntity(name, quantity, price, createdAt, updatedAt);
    }
}

