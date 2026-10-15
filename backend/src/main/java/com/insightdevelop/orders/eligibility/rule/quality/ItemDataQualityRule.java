package com.insightdevelop.orders.eligibility.rule.quality;

import com.insightdevelop.orders.domain.OrderItem;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Warning: an item has a missing/non-positive quantity or a missing price; it is ignored in totals. */
@Component
public class ItemDataQualityRule implements EligibilityRule {

    @Override
    public String id() {
        return "ITEM_DATA_QUALITY";
    }

    @Override
    public List<Finding> evaluate(EvaluationContext context) {
        List<Finding> findings = new ArrayList<>();
        for (OrderItem item : context.items()) {
            String label = item.sku() != null ? item.sku() : String.valueOf(item.name());
            if (item.quantity() == null || item.quantity() <= 0) {
                findings.add(Finding.warning(id(), "Item " + label + " has an invalid quantity ("
                        + item.quantity() + "); it is ignored in totals"));
            } else if (item.unitPrice() == null) {
                findings.add(Finding.warning(id(), "Item " + label + " has no unit price; it is ignored in the items total"));
            }
        }
        return findings;
    }
}
