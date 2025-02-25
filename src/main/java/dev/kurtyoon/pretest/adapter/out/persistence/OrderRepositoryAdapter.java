package dev.kurtyoon.pretest.adapter.out.persistence;

import dev.kurtyoon.pretest.adapter.out.persistence.entity.OrderEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.entity.OrderItemEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.entity.ProductEntity;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.OrderJpaRepository;
import dev.kurtyoon.pretest.adapter.out.persistence.repository.ProductJpaRepository;
import dev.kurtyoon.pretest.application.port.out.OrderRepositoryPort;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.Order;
import dev.kurtyoon.pretest.domain.OrderItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adapter
public class OrderRepositoryAdapter implements OrderRepositoryPort {

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
        OrderEntity orderEntity = toEntity(order);
        OrderEntity savedOrder = orderJpaRepository.save(orderEntity);
        return toDomain(savedOrder);
    }

    @Override
    public List<Order> saveAllOrder(List<Order> orderList) {
        List<OrderEntity> orderEntityList = orderList.stream()
                .map(this::toEntity)
                .toList();

        List<OrderEntity> savedOrders = orderJpaRepository.saveAll(orderEntityList);

        return savedOrders.stream()
                .map(this::toDomain)
                .toList();
    }

    private OrderEntity toEntity(Order order) {
        List<OrderItemEntity> orderItemEntityList = order.getItems().stream()
                .map(this::toOrderItemEntity)
                .toList();

        return OrderEntity.create(
                order.getCustomerName(),
                order.getCustomerAddress(),
                orderItemEntityList
        );
    }

    private OrderItemEntity toOrderItemEntity(OrderItem orderItem) {
        ProductEntity productEntity = productJpaRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_PRODUCT));

        return OrderItemEntity.create(
            productEntity, orderItem.getQuantity()
        );
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
                entity.getQuantity()
        );
    }
}
