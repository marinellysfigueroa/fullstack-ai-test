package com.insightdevelop.orders.eligibility;

/**
 * One observation produced by a rule.
 *
 * @param penalty points deducted from the score (only non-zero for {@link Severity#PENALTY})
 */
public record Finding(String ruleId, Severity severity, String message, int penalty) {

    public static Finding blocking(String ruleId, String message) {
        return new Finding(ruleId, Severity.BLOCKING, message, 0);
    }

    public static Finding penalty(String ruleId, int points, String message) {
        if (points <= 0) {
            throw new IllegalArgumentException("Penalty must be positive: " + points);
        }
        return new Finding(ruleId, Severity.PENALTY, message, points);
    }

    public static Finding warning(String ruleId, String message) {
        return new Finding(ruleId, Severity.WARNING, message, 0);
    }
}
