package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.domain.model.Order;

import java.util.List;

public interface OrderRepositoryPort {
    Order saveOrder(Order order);
    List<Order> saveAllOrder(List<Order> orderList);
}
