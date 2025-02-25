package dev.kurtyoon.pretest.domain;

import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;

import java.time.LocalDateTime;

public class Product {

    private final Long id;
    private final String name;
    private int quantity;
    private int price;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    /* -------------------------------------------------- */
    /* Constructor -------------------------------------- */
    /* -------------------------------------------------- */
    public Product(
            Long id,
            String name,
            int quantity,
            int price,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = (name != null) ? name : "";
        this.quantity = quantity;
        this.price = price;
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

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
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
    public void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new CommonException(ErrorCode.INVALID_QUANTITY);
        }

        if (this.quantity < quantity) {
            throw new CommonException(ErrorCode.OUT_OF_STOCK);
        }

        this.quantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /* -------------------------------------------------- */
    /* Static Factory Method ---------------------------- */
    /* -------------------------------------------------- */
    public static Product create(
            Long id,
            String name,
            int quantity,
            int price,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Product(
                id,
                name,
                quantity,
                price,
                createdAt,
                updatedAt
        );
    }
}
