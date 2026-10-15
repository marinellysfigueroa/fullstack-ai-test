package com.insightdevelop.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Immutable order snapshot. Raw data is kept exactly as received (nullable fields,
 * inconsistent totals, missing risk flags); interpretation happens in the eligibility rules.
 *
 * @param riskFlags {@code null} means the risk assessment is missing, which is different from an empty list.
 * @param version   incremented on every change; used to detect concurrent modifications.
 */
public record Order(
        String orderId,
        Customer customer,
        List<OrderItem> items,
        BigDecimal totalAmount,
        Shipping shipping,
        List<String> riskFlags,
        InventoryStatus inventoryStatus,
        OrderStatus status,
        Instant createdAt,
        ExpeditedApproval expeditedApproval,
        long version) {

    public Order {
        items = items == null ? null : List.copyOf(items);
        riskFlags = riskFlags == null ? null : List.copyOf(riskFlags);
    }

    public Order approveExpedited(ExpeditedApproval approval) {
        return new Order(orderId, customer, items, totalAmount, shipping, riskFlags, inventoryStatus,
                OrderStatus.APPROVED, createdAt, approval, version + 1);
    }

    public Order withInventoryStatus(InventoryStatus newInventoryStatus) {
        return new Order(orderId, customer, items, totalAmount, shipping, riskFlags, newInventoryStatus,
                status, createdAt, expeditedApproval, version + 1);
    }
}
