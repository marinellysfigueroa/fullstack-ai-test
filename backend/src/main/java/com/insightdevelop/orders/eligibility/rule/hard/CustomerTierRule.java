package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Hard rule: the customer must have a tier. */
@Component
public class CustomerTierRule implements HardRule {

    @Override
    public String id() {
        return "CUSTOMER_TIER";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        if (context.order().customer() == null) {
            return Optional.of("Customer information is missing");
        }
        return context.tier().isPresent() ? Optional.empty() : Optional.of("Customer tier is not assigned");
    }
}
