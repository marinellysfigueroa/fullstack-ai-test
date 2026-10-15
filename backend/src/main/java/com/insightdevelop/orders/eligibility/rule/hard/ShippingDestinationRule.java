package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Hard rule (carried over from the previous version): shipping must target a supported country. */
@Component
public class ShippingDestinationRule implements HardRule {

    @Override
    public String id() {
        return "SHIPPING_DESTINATION";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        Shipping shipping = context.order().shipping();
        if (shipping == null) {
            return Optional.of("Shipping information is missing");
        }
        if (shipping.country() == null || shipping.country().isBlank()) {
            return Optional.of("Shipping country is missing");
        }
        String country = shipping.country().trim().toUpperCase(Locale.ROOT);
        return context.properties().supportedCountries().contains(country)
                ? Optional.empty()
                : Optional.of("Expedited shipping is not offered for country " + country);
    }
}
