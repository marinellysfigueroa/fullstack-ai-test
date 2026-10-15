package com.insightdevelop.orders.api.dto;

import com.insightdevelop.orders.eligibility.Decision;
import com.insightdevelop.orders.eligibility.EligibilityResult;
import com.insightdevelop.orders.eligibility.Finding;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Eligibility contract. {@code eligible}, {@code score}, {@code reasons} and {@code warnings} are the
 * required fields; the rest lets the UI distinguish blocking issues and risk levels without
 * re-implementing any rule.
 */
public record EligibilityResponse(
        @Schema(description = "True only if no hard rule fails and score >= 60")
        boolean eligible,
        @Schema(description = "100 minus penalties, never below 0", minimum = "0", maximum = "100", example = "70")
        int score,
        @Schema(description = "Blocking reasons first, then score deductions")
        List<String> reasons,
        @Schema(description = "Missing or inconsistent data that did not change eligibility by itself")
        List<String> warnings,
        @Schema(description = "ELIGIBLE, ELIGIBLE_WITH_RISK (score < 80 or warnings present) or NOT_ELIGIBLE")
        Decision decision,
        @Schema(description = "Subset of reasons that make the order ineligible")
        List<String> blockingReasons,
        @Schema(description = "Every rule finding with its severity")
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
