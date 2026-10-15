package com.insightdevelop.orders.eligibility.rule.score;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.ScoringRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Score: -30 when the total quantity exceeds the configured limit (3). */
@Component
public class HighQuantityRule implements ScoringRule {

    @Override
    public String id() {
        return "HIGH_QUANTITY";
    }

    @Override
    public int penalty() {
        return 30;
    }

    @Override
    public Optional<String> deduction(EvaluationContext context) {
        int quantity = context.totalQuantity();
        int limit = context.properties().maxQuantityWithoutPenalty();
        return quantity > limit
                ? Optional.of("Total quantity " + quantity + " exceeds " + limit)
                : Optional.empty();
    }
}
