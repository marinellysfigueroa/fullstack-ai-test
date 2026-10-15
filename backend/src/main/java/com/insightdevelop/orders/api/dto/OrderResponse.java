package com.insightdevelop.orders.api.dto;

import com.insightdevelop.orders.domain.Customer;
import com.insightdevelop.orders.domain.ExpeditedApproval;
import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.OrderItem;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.service.OrderEvaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** An order as returned by the API, always together with its current eligibility. */
public record OrderResponse(
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
        long version,
        EligibilityResponse eligibility,
        boolean canApproveExpedited) {

    public static OrderResponse from(OrderEvaluation evaluation) {
        var o = evaluation.order();
        return new OrderResponse(o.orderId(), o.customer(), o.items(), o.totalAmount(), o.shipping(), o.riskFlags(),
                o.inventoryStatus(), o.status(), o.createdAt(), o.expeditedApproval(), o.version(),
                EligibilityResponse.from(evaluation.eligibility()), evaluation.canApproveExpedited());
    }
}
