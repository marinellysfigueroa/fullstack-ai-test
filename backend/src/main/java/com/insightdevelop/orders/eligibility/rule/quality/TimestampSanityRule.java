package com.insightdevelop.orders.eligibility.rule.quality;

import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.Finding;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Warning: createdAt is missing, or a timestamp lies in the future (clock skew or bad data). */
@Component
public class TimestampSanityRule implements EligibilityRule {

    @Override
    public String id() {
        return "TIMESTAMP_SANITY";
    }

    @Override
    public List<Finding> evaluate(EvaluationContext context) {
        List<Finding> findings = new ArrayList<>();
        Instant createdAt = context.order().createdAt();
        if (createdAt == null) {
            findings.add(Finding.warning(id(), "createdAt is missing; order age could not be checked"));
        } else if (createdAt.isAfter(context.now())) {
            findings.add(Finding.warning(id(), "createdAt is in the future"));
        }
        InventoryStatus inventory = context.order().inventoryStatus();
        if (inventory != null && inventory.lastCheckedAt() != null && inventory.lastCheckedAt().isAfter(context.now())) {
            findings.add(Finding.warning(id(), "Inventory lastCheckedAt is in the future"));
        }
        return findings;
    }
}
