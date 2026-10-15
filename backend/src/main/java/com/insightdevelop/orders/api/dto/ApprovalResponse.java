package com.insightdevelop.orders.api.dto;

import com.insightdevelop.orders.domain.ExpeditedApproval;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.service.ApprovalOutcome;

/**
 * Successful approval.
 *
 * @param alreadyApproved {@code true} when this call was a repeat of an earlier successful approval
 * @param eligibility     the re-evaluation performed for this approval; {@code null} on a repeat
 */
public record ApprovalResponse(
        String orderId,
        OrderStatus status,
        boolean alreadyApproved,
        ExpeditedApproval approval,
        EligibilityResponse eligibility) {

    public static ApprovalResponse from(ApprovalOutcome outcome) {
        return new ApprovalResponse(outcome.order().orderId(), outcome.order().status(), outcome.alreadyApproved(),
                outcome.order().expeditedApproval(), EligibilityResponse.from(outcome.eligibility()));
    }
}
