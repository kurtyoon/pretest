package dev.kurtyoon.pretest.domain.service;

import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import dev.kurtyoon.pretest.domain.model.Order;
import dev.kurtyoon.pretest.domain.model.OrderItem;
import dev.kurtyoon.pretest.domain.model.Product;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerUtils.getLogger(OrderService.class);

    public void validateOrderWithProductList(
            Order order,
            List<Product> productList
    ) {
        // 1. ProductId -> Product 매핑
        Map<Long, Product> productMap = productList.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        // 2. 각 OrderItem 에 대한 재고 검증
        for (OrderItem orderItem : order.getItems()) {
            Product product = productMap.get(orderItem.getProductId());

            // 상품이 존재하지 않는 경우
            if (product == null) {
                throw new CommonException(ErrorCode.NOT_FOUND_PRODUCT);
            }

            // 재고가 부족한 경우
            if (product.getQuantity() < orderItem.getQuantity()) {
                throw new CommonException(ErrorCode.OUT_OF_STOCK);
            }
        }
    }
}
