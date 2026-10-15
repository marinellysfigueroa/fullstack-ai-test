package com.insightdevelop.orders.eligibility;

import java.time.Instant;
import java.util.List;

/**
 * Outcome of evaluating one order.
 *
 * @param reasons         everything that lowered or blocked eligibility (blocking reasons first, then deductions)
 * @param blockingReasons the subset of reasons that make the order ineligible
 * @param warnings        data-quality notes that did not change eligibility by themselves
 * @param findings        the full, structured list of rule findings
 */
public record EligibilityResult(
        boolean eligible,
        int score,
        Decision decision,
        List<String> reasons,
        List<String> blockingReasons,
        List<String> warnings,
        List<Finding> findings,
        Instant evaluatedAt) {

    public EligibilityResult {
        reasons = List.copyOf(reasons);
        blockingReasons = List.copyOf(blockingReasons);
        warnings = List.copyOf(warnings);
        findings = List.copyOf(findings);
    }
}
