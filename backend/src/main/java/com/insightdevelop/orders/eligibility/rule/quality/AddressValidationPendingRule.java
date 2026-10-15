package com.insightdevelop.orders.eligibility.rule.quality;

import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Component;

import java.util.List;

/** Warning: address validation has not been performed (addressValidated = null). */
@Component
public class AddressValidationPendingRule implements EligibilityRule {

    @Override
    public String id() {
        return "ADDRESS_VALIDATION_PENDING";
    }

    @Override
    public List<Finding> evaluate(EvaluationContext context) {
        Shipping shipping = context.order().shipping();
        return shipping != null && shipping.addressValidated() == null
                ? List.of(Finding.warning(id(), "Shipping address has not been validated yet"))
                : List.of();
    }
}
