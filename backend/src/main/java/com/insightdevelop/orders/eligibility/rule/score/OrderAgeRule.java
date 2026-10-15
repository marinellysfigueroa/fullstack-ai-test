package com.insightdevelop.orders.eligibility.rule.score;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.ScoringRule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Score: -10 when the order was created more than the configured period (48h) ago. */
@Component
public class OrderAgeRule implements ScoringRule {

    @Override
    public String id() {
        return "ORDER_AGE";
    }

    @Override
    public int penalty() {
        return 10;
    }

    @Override
    public Optional<String> deduction(EvaluationContext context) {
        Instant createdAt = context.order().createdAt();
        if (createdAt == null) {
            return Optional.empty(); // reported as a warning by TimestampSanityRule
        }
        Duration age = Duration.between(createdAt, context.now());
        Duration limit = context.properties().orderAgePenaltyAfter();
        return age.compareTo(limit) > 0
                ? Optional.of("Order is " + age.toHours() + "h old (older than " + limit.toHours() + "h)")
                : Optional.empty();
    }
}
