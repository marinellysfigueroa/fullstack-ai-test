package com.insightdevelop.orders.service;

import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.eligibility.EligibilityResult;

/** An order together with its freshly computed eligibility. */
public record OrderEvaluation(Order order, EligibilityResult eligibility) {

    /** Whether the UI should offer the approve action; computed here so the frontend never derives it. */
    public boolean canApproveExpedited() {
        return eligibility.eligible() && order.status() != OrderStatus.APPROVED;
    }
}
