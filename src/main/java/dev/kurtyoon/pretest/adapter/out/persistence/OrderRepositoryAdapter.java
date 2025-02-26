package dev.kurtyoon.pretest.adapter.out.persistence;

import dev.kurtyoon.pretest.adapter.out.persistence.entity.OrderEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.entity.OrderItemEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.entity.ProductEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.OrderJpaRepository;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.ProductJpaRepository;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Adapter
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private static final Logger log = LoggerUtils.getLogger(OrderRepositoryAdapter.class);

    private final OrderJpaRepository orderJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    public OrderRepositoryAdapter(
            OrderJpaRepository orderJpaRepository,
            ProductJpaRepository productJpaRepository
    ) {
        this.orderJpaRepository = orderJpaRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    @Transactional
    public Order saveOrder(Order order) {
        log.debug("Saving single order: Customer = {}, items = {}", order.getCustomerName(), order.getItems());
        OrderEntity savedOrder = orderJpaRepository.save(toEntity(order));
        return toDomain(savedOrder);
    }

    @Override
    @Transactional
    public List<Order> saveAllOrder(List<Order> orderList) {

        if (orderList.isEmpty()) {
            log.debug("No orders to save");
            return List.of();
        }

        log.debug("Saving bulk orders: {} total", orderList.size());
        List<OrderEntity> savedEntityList = orderJpaRepository.saveAll(toEntityList(orderList));
        return toDomainList(savedEntityList);
    }

    private OrderEntity toEntity(Order order) {
        List<OrderItemEntity> orderItemEntityList = order.getItems().stream()
                .map(this::toOrderItemEntity)
                .toList();

        return OrderEntity.create(
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getTotalPrice(),
                order.getOrderedAt(),
                orderItemEntityList
        );
    }

    private OrderItemEntity toOrderItemEntity(OrderItem orderItem) {
        ProductEntity productEntity = productJpaRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_PRODUCT));

        return OrderItemEntity.create(
            productEntity, orderItem.getQuantity(), orderItem.getPrice(), orderItem.getTotalPrice()
        );
    }

    private List<OrderEntity> toEntityList(List<Order> orderList) {

        // 1. 주문에서 상품 Id 목록 추출
        Set<Long> productIdSet = extractAllProductIdSet(orderList);

        // 2. 주문에 포함된 상품 일괄 조회
        Map<Long, ProductEntity> productEntityMap = fetchProductEntities(productIdSet);

        // 3. 주문 도메인을 엔티티로 전환
        return orderList.stream()
                .map(order -> toEntityWithProductMap(order, productEntityMap))
                .toList();
    }

    private Set<Long> extractAllProductIdSet(List<Order> orderList) {
        return orderList.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
    }

    private OrderEntity toEntityWithProductMap(
            Order order,
            Map<Long, ProductEntity> productEntityMap
    ) {
        List<OrderItemEntity> itemEntityList = order.getItems().stream()
                .map(item -> createOrderItemEntity(item, productEntityMap))
                .toList();

        return OrderEntity.create(
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getTotalPrice(),
                order.getOrderedAt(),
                itemEntityList
        );
    }

    private OrderItemEntity createOrderItemEntity(
            OrderItem item,
            Map<Long, ProductEntity> productEntityMap
    ) {
        ProductEntity productEntity = productEntityMap.get(item.getProductId());

        if (productEntity == null) {
            log.error("Product not found while creating order item: Id = {}", item.getId());
            throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        return OrderItemEntity.create(
                productEntity,
                item.getQuantity(),
                item.getPrice(),
                item.getTotalPrice()
        );
    }

    private Map<Long, ProductEntity> fetchProductEntities(Set<Long> productIdSet) {
        List<ProductEntity> productEntityList = productJpaRepository.findAllById(productIdSet);

        // 모든 상품이 존재하는지 검증
        if (productEntityList.size() < productIdSet.size()) {
            Set<Long> foundIdSet = productEntityList.stream()
                    .map(ProductEntity::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIdSet = productIdSet.stream()
                    .filter(id -> !foundIdSet.contains(id))
                    .collect(Collectors.toSet());

            log.error("Missing products detected during order processing: {}", missingIdSet);
            throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        return productEntityList.stream()
                .collect(Collectors.toMap(ProductEntity::getId, product -> product));
    }

    private List<Order> toDomainList(List<OrderEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> orderItemList = entity.getOrderItems().stream()
                .map(this::toDomainOrderItem)
                .toList();

        return Order.create(
                entity.getId(),
                entity.getCustomerName(),
                entity.getCustomerAddress(),
                orderItemList
        );
    }

    private OrderItem toDomainOrderItem(OrderItemEntity entity) {
        return OrderItem.create(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getProduct().getName(),
                entity.getQuantity(),
                entity.getPrice()
        );
    }
}
