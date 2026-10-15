package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/** Hard rule (carried over from the previous version): some risk flags (e.g. FRAUD_SUSPECTED) block expedited processing. */
@Component
public class BlockingRiskFlagRule implements HardRule {

    @Override
    public String id() {
        return "BLOCKING_RISK_FLAG";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        String blocking = context.riskFlags().stream()
                .filter(context.properties().blockingRiskFlags()::contains)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return blocking.isEmpty()
                ? Optional.empty()
                : Optional.of("Blocking risk flag(s) present: " + blocking);
    }
}
