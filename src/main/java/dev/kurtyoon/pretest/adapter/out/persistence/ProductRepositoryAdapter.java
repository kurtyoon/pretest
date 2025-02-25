package dev.kurtyoon.pretest.adapter.out.persistence;

import dev.kurtyoon.pretest.adapter.out.persistence.entity.ProductEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.ProductJpaRepository;
import dev.kurtyoon.pretest.application.port.out.ProductRepositoryPort;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.domain.Product;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adapter
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Product findById(Long productId) {
        return productJpaRepository.findById(productId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Product> findAllByIdList(List<Long> productIdList) {
        List<ProductEntity> entities = productJpaRepository.findAllById(productIdList);

        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Product saveProduct(Product product) {

        ProductEntity entity = productJpaRepository.findById(product.getId())
                .map(existingEntity -> updateEntity(existingEntity, product))
                .orElseGet(() -> toEntity(product));

        return toDomain(productJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public List<Product> saveAllProducts(List<Product> productList) {

        List<ProductEntity> entityList = productList.stream()
                .map(product -> productJpaRepository.findById(product.getId())
                        .map(existingEntity -> updateEntity(existingEntity, product))
                        .orElseGet(() -> toEntity(product))
                ).toList();

        return productJpaRepository.saveAll(entityList).stream()
                .map(this::toDomain)
                .toList();
    }

    private Product toDomain(ProductEntity entity) {
        return Product.create(entity.getId(), entity.getName(), entity.getQuantity(), entity.getPrice());
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
        entity.updateQuantity(product.getQuantity());
        return entity;
    }

}
