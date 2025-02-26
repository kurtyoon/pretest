package dev.kurtyoon.pretest.application.service.support;

import dev.kurtyoon.pretest.domain.OrderItem;
import dev.kurtyoon.pretest.domain.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderExecutionContext {

    private final List<Long> productIdList;
    private final List<Long> acquiredLockList = new ArrayList<>();
    private final Map<Long, Integer> originalStockMap = new HashMap<>();

    public OrderExecutionContext(List<Long> productIdList) {
        this.productIdList = productIdList;
    }

    public List<Long> getProductIdList() {
        return productIdList;
    }

    public List<Long> getAcquiredLockList() {
        return acquiredLockList;
    }

    public void lockAcquired(Long productId) {
        acquiredLockList.add(productId);
    }

    public void backupStockState(Map<Long, Product> productMap) {
        for (Map.Entry<Long, Product> entry : productMap.entrySet()) {
            originalStockMap.put(entry.getKey(), entry.getValue().getQuantity());
        }
    }

    public void restoreState(Map<Long, Product> productMap) {
        for (Map.Entry<Long, Integer> entry : originalStockMap.entrySet()) {
            Product product = productMap.get(entry.getKey());
            product.updateQuantity(entry.getValue());
        }
    }

    public Integer getOriginalStock(Long productId) {
        return originalStockMap.get(productId);
    }

    public boolean validateAndReduceStock(
            List<OrderItem> orderItemList,
            Map<Long, Product> productMap
    ) {

        for (OrderItem item : orderItemList) {
            Product product = productMap.get(item.getProductId());

            if (product == null || product.getQuantity() < item.getQuantity()) {
                return false;
            }
        }

        for (OrderItem item : orderItemList) {
            Product product = productMap.get(item.getProductId());
            product.reduceStock(item.getQuantity());
        }

        return true;
    }
}
