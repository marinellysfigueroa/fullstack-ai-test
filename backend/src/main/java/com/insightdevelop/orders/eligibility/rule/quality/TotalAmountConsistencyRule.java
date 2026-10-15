package com.insightdevelop.orders.eligibility.rule.quality;

import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Warning: totalAmount is missing or does not match the sum of the items.
 * Threshold checks then use the larger of both values (see {@link EvaluationContext#effectiveAmount()}).
 */
@Component
public class TotalAmountConsistencyRule implements EligibilityRule {

    @Override
    public String id() {
        return "TOTAL_AMOUNT_CONSISTENCY";
    }

    @Override
    public List<Finding> evaluate(EvaluationContext context) {
        if (context.items().isEmpty()) {
            return List.of(); // nothing to compare against; ItemsPresentRule blocks the order anyway
        }
        BigDecimal declared = context.order().totalAmount();
        BigDecimal computed = context.itemsTotal();
        if (declared == null) {
            return List.of(Finding.warning(id(),
                    "totalAmount is missing; using the items total of " + computed.toPlainString()));
        }
        if (declared.subtract(computed).abs().compareTo(context.properties().amountTolerance()) > 0) {
            return List.of(Finding.warning(id(), "totalAmount " + declared.toPlainString()
                    + " does not match the items total " + computed.toPlainString()
                    + "; using " + context.effectiveAmount().toPlainString() + " for threshold checks"));
        }
        return List.of();
    }
}
