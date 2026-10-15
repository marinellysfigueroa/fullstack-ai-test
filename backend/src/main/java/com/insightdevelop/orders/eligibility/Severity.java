package com.insightdevelop.orders.eligibility;

public enum Severity {
    /** A hard rule failed: the order is not eligible regardless of score. */
    BLOCKING,
    /** A scoring rule deducted points. */
    PENALTY,
    /** Informational: missing or inconsistent data that did not change eligibility by itself. */
    WARNING
}
