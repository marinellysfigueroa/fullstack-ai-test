package com.insightdevelop.orders.eligibility.rule.score;

import com.insightdevelop.orders.domain.CustomerTier;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.ScoringRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** Score: -20 when the order amount exceeds the expected threshold for the customer tier. */
@Component
public class TierAmountThresholdRule implements ScoringRule {

    @Override
    public String id() {
        return "TIER_AMOUNT_THRESHOLD";
    }

    @Override
    public int penalty() {
        return 20;
    }

    @Override
    public Optional<String> deduction(EvaluationContext context) {
        Optional<CustomerTier> tier = context.tier();
        if (tier.isEmpty()) {
            return Optional.empty(); // a missing tier is a hard failure handled by CustomerTierRule
        }
        BigDecimal threshold = context.properties().tierAmountThresholds().get(tier.get());
        BigDecimal amount = context.effectiveAmount();
        if (threshold == null || amount.compareTo(threshold) <= 0) {
            return Optional.empty();
        }
        return Optional.of("Amount " + amount.toPlainString() + " exceeds the " + tier.get()
                + " threshold of " + threshold.toPlainString());
    }
}
