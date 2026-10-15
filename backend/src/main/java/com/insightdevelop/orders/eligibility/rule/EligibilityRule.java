package com.insightdevelop.orders.eligibility.rule;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;

import java.util.List;

/**
 * Extension point of the rule system.
 * <p>
 * Every Spring bean implementing this interface is picked up automatically by the
 * {@link com.insightdevelop.orders.eligibility.EligibilityEngine}. Adding a rule therefore means
 * adding a new {@code @Component} class; no existing rule or the engine needs to change.
 * Most rules should implement one of the convenience interfaces {@link HardRule} or {@link ScoringRule}.
 */
public interface EligibilityRule {

    /** Stable identifier, used in API output and tests. */
    String id();

    /** Returns zero or more findings; an empty list means the rule has nothing to report. */
    List<Finding> evaluate(EvaluationContext context);
}
