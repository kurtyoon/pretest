package dev.kurtyoon.pretest.adapter.out.persistence.repository;

import dev.kurtyoon.pretest.adapter.out.persistence.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {
}
