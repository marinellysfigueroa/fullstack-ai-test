package com.insightdevelop.orders.eligibility.rule;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;

import java.util.List;
import java.util.Optional;

/** A rule that, when violated, makes the order ineligible regardless of its score. */
public interface HardRule extends EligibilityRule {

    /** Returns the reason the order violates this rule, or empty when it passes. */
    Optional<String> violation(EvaluationContext context);

    @Override
    default List<Finding> evaluate(EvaluationContext context) {
        return violation(context).map(reason -> List.of(Finding.blocking(id(), reason))).orElse(List.of());
    }
}
