package com.insightdevelop.orders.eligibility.rule.quality;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Component;

import java.util.List;

/** Warning: riskFlags is missing (not just empty), so the order was evaluated as if it had no flags. */
@Component
public class MissingRiskAssessmentRule implements EligibilityRule {

    @Override
    public String id() {
        return "RISK_ASSESSMENT_MISSING";
    }

    @Override
    public List<Finding> evaluate(EvaluationContext context) {
        return context.order().riskFlags() == null
                ? List.of(Finding.warning(id(), "Risk assessment is missing; evaluated as having no risk flags"))
                : List.of();
    }
}
