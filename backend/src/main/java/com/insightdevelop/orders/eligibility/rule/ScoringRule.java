package com.insightdevelop.orders.eligibility.rule;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;

import java.util.List;
import java.util.Optional;

/** A rule that deducts a fixed number of points from the score when it applies. */
public interface ScoringRule extends EligibilityRule {

    int penalty();

    /** Returns the explanation for the deduction when the rule applies, or empty otherwise. */
    Optional<String> deduction(EvaluationContext context);

    @Override
    default List<Finding> evaluate(EvaluationContext context) {
        return deduction(context)
                .map(reason -> List.of(Finding.penalty(id(), penalty(), reason + " (-" + penalty() + ")")))
                .orElse(List.of());
    }
}
