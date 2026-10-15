package com.insightdevelop.orders.eligibility;

public enum Decision {
    /** Eligible with no relevant risk signals. */
    ELIGIBLE,
    /** Eligible, but the score is low or there are data-quality warnings. */
    ELIGIBLE_WITH_RISK,
    /** A hard rule failed or the score is below the minimum. */
    NOT_ELIGIBLE
}
