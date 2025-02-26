package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.domain.Product;

import java.util.List;

public interface ProductRepositoryPort {

    /**
     * 상품을 조회합니다.
     * @param productId 상품 ID
     * @return 상품
     */
    Product findById(Long productId);

    /**
     * 상품 목록을 조회합니다.
     * @param productIdList 상품 ID 목록
     * @return 상품 목록
     */
    List<Product> findAllByIdList(List<Long> productIdList);

    /**
     * 상품 목록을 저장합니다.
     * @param productList 상품 목록
     */
    void saveAllProducts(List<Product> productList);
}
