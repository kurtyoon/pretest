package dev.kurtyoon.pretest.adapter.out.persistence;

import dev.kurtyoon.pretest.adapter.out.persistence.entity.ProductEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.ProductJpaRepository;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Product;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Adapter
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private static final Logger log = LoggerUtils.getLogger(ProductRepositoryAdapter.class);

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Product findById(Long productId) {
        log.debug("Finding product by ID: {}", productId);
        return productJpaRepository.findById(productId)
                .map(this::toDomain)
                .orElseThrow(() -> {
                    log.error("Product not found: ID: {}", productId);
                    return new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
                });
    }

    @Override
    public List<Product> findAllByIdList(List<Long> productIdList) {

        if (productIdList.isEmpty()) {
            log.debug("No product Id list to find");
            return List.of();
        }

        log.debug("Finding products in bulk: {} Id list", productIdList.size());
        List<ProductEntity> entities = productJpaRepository.findAllById(productIdList);

        if (entities.size() < productIdList.size()) {
            List<Long> foundIdList = entities.stream()
                    .map(ProductEntity::getId)
                    .toList();

            List<Long> missingIdList = productIdList.stream()
                    .filter(id -> !foundIdList.contains(id))
                    .toList();

            log.debug("Some products not found: {}", missingIdList);
        }

        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveAllProducts(List<Product> productList) {

        if (productList.isEmpty()) {
            log.debug("No products to save");
            return;
        }

        log.debug("Saving/updating products in bulk: {} items", productList.size());

        // 1. 상품 Id 리스트 추출
        List<Long> productIdList = productList.stream()
                .map(Product::getId)
                .toList();

        // 2. 기존 상품 엔티티 일괄 조회
        Map<Long, ProductEntity> existingProducts = productJpaRepository.findAllById(productIdList).stream()
                .collect(Collectors.toMap(ProductEntity::getId, entity -> entity));

        // 3. 각 상품에 대해 업데이트 또는 생성
        List<ProductEntity> entitiesToSave = productList.stream()
                .map(product -> updateOrCreateEntity(product, existingProducts))
                .toList();

        // 4. 일괄 저장
        productJpaRepository.saveAll(entitiesToSave);
        log.debug("Products saved successfully: {} items", entitiesToSave.size());

        List<ProductEntity> entityList = productList.stream()
                .map(product -> productJpaRepository.findById(product.getId())
                        .map(existingEntity -> updateEntity(existingEntity, product))
                        .orElseGet(() -> toEntity(product))
                ).toList();

        productJpaRepository.saveAll(entityList);
    }

    private ProductEntity updateOrCreateEntity(
            Product product,
            Map<Long, ProductEntity> existingProducts
    ) {
        ProductEntity existingEntity = existingProducts.get(product.getId());

        if (existingEntity != null) {
            log.trace("Updating proudct: Id = {}, quantity = {} -> {}",
                    product.getId(), existingEntity.getQuantity(), product.getQuantity());

            return updateEntity(existingEntity, product);
        } else {
            log.trace("Creating new product: Id = {}, quantity = {}", product.getId(), product.getQuantity());
            return toEntity(product);
        }
    }

    private Product toDomain(ProductEntity entity) {
        return Product.create(
                entity.getId(),
                entity.getName(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProductEntity toEntity(Product product) {
        return ProductEntity.create(
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private ProductEntity updateEntity(ProductEntity entity, Product product) {

        if (entity.getQuantity() != product.getQuantity()) {
            log.debug("Product stock changed: Id = {}, name = {}, quantity = {} -> {}",
                    entity.getId(), entity.getName(), entity.getQuantity(), product.getQuantity());
        }

        entity.updateQuantity(product.getQuantity(), product.getUpdatedAt());
        return entity;
    }

}
