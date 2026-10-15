package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Hard rule: inventory must be reported and available. */
@Component
public class InventoryAvailableRule implements HardRule {

    @Override
    public String id() {
        return "INVENTORY_AVAILABLE";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        InventoryStatus inventory = context.order().inventoryStatus();
        if (inventory == null) {
            return Optional.of("Inventory status is missing");
        }
        return inventory.available() ? Optional.empty() : Optional.of("Inventory is not available");
    }
}
