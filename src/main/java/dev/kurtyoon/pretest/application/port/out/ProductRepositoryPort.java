package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.domain.Product;

import java.util.List;

public interface ProductRepositoryPort {

    Product findById(Long productId);

    List<Product> findAllByIdList(List<Long> productIdList);

    void saveAllProducts(List<Product> productList);
}
