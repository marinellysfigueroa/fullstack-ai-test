package com.insightdevelop.orders.eligibility.rule.score;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.ScoringRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Score: -25 when riskFlags contains HIGH_VALUE. */
@Component
public class HighValueRiskFlagRule implements ScoringRule {

    @Override
    public String id() {
        return "HIGH_VALUE_RISK_FLAG";
    }

    @Override
    public int penalty() {
        return 25;
    }

    @Override
    public Optional<String> deduction(EvaluationContext context) {
        return context.riskFlags().contains("HIGH_VALUE")
                ? Optional.of("Risk flag HIGH_VALUE present")
                : Optional.empty();
    }
}
