package com.insightdevelop.orders.api.dto;

import com.insightdevelop.orders.eligibility.Decision;
import com.insightdevelop.orders.eligibility.EligibilityResult;
import com.insightdevelop.orders.eligibility.Finding;

import java.time.Instant;
import java.util.List;

/**
 * Eligibility contract. {@code eligible}, {@code score}, {@code reasons} and {@code warnings} are the
 * required fields; the rest lets the UI distinguish blocking issues and risk levels without
 * re-implementing any rule.
 */
public record EligibilityResponse(
        boolean eligible,
        int score,
        List<String> reasons,
        List<String> warnings,
        Decision decision,
        List<String> blockingReasons,
        List<Finding> findings,
        Instant evaluatedAt) {

    public static EligibilityResponse from(EligibilityResult result) {
        if (result == null) {
            return null;
        }
        return new EligibilityResponse(result.eligible(), result.score(), result.reasons(), result.warnings(),
                result.decision(), result.blockingReasons(), result.findings(), result.evaluatedAt());
    }
}
