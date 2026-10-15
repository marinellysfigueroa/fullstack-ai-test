package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** Hard rule: inventory must have been checked within the configured window (10 minutes by default). */
@Component
public class InventoryFreshnessRule implements HardRule {

    @Override
    public String id() {
        return "INVENTORY_FRESH";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        InventoryStatus inventory = context.order().inventoryStatus();
        if (inventory == null) {
            return Optional.empty(); // reported by InventoryAvailableRule
        }
        if (inventory.lastCheckedAt() == null) {
            return Optional.of("Inventory has never been checked, availability cannot be trusted");
        }
        Duration age = Duration.between(inventory.lastCheckedAt(), context.now());
        Duration maxAge = context.properties().inventoryMaxAge();
        if (age.compareTo(maxAge) > 0) {
            return Optional.of("Inventory check is stale: last checked " + age.toMinutes()
                    + " min ago (maximum " + maxAge.toMinutes() + " min)");
        }
        return Optional.empty();
    }
}
