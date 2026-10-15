package com.insightdevelop.orders.service;

import com.insightdevelop.orders.eligibility.EligibilityResult;

/** The approval request is valid but the order cannot be approved in its current state. */
public class ApprovalRejectedException extends RuntimeException {

    public enum Reason {
        /** Re-evaluation found the order ineligible. */
        NOT_ELIGIBLE,
        /** The order is in a status that does not allow approval (e.g. REJECTED). */
        INVALID_STATUS
    }

    private final String orderId;
    private final Reason reason;
    private final EligibilityResult eligibility;

    public ApprovalRejectedException(String orderId, Reason reason, String message, EligibilityResult eligibility) {
        super(message);
        this.orderId = orderId;
        this.reason = reason;
        this.eligibility = eligibility;
    }

    public String orderId() {
        return orderId;
    }

    public Reason reason() {
        return reason;
    }

    public EligibilityResult eligibility() {
        return eligibility;
    }
}
