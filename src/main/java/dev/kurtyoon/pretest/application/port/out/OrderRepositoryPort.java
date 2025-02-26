package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.domain.Order;

import java.util.List;

public interface OrderRepositoryPort {

    /**
     * 주문을 저장합니다.
     * @param order 주문
     * @return 저장된 주문
     */
    Order saveOrder(Order order);

    /**
     * 주문 목록을 저장합니다.
     * @param orderList 주문 목록
     * @return 저장된 주문 목록
     */
    List<Order> saveAllOrder(List<Order> orderList);
}
