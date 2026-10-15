package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Hard rule: the order must contain at least one item. */
@Component
public class ItemsPresentRule implements HardRule {

    @Override
    public String id() {
        return "ITEMS_PRESENT";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        return context.items().isEmpty() ? Optional.of("Order has no items") : Optional.empty();
    }
}
