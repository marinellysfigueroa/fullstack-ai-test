package com.insightdevelop.orders.eligibility.rule.score;

import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.ScoringRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Score: -15 when address validation explicitly failed (null is a warning, not a penalty). */
@Component
public class AddressNotValidatedRule implements ScoringRule {

    @Override
    public String id() {
        return "ADDRESS_NOT_VALIDATED";
    }

    @Override
    public int penalty() {
        return 15;
    }

    @Override
    public Optional<String> deduction(EvaluationContext context) {
        Shipping shipping = context.order().shipping();
        return shipping != null && Boolean.FALSE.equals(shipping.addressValidated())
                ? Optional.of("Shipping address failed validation")
                : Optional.empty();
    }
}
