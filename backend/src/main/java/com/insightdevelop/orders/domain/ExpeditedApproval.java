package com.insightdevelop.orders.domain;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot recorded when an order is approved for expedited processing, so repeated
 * approval calls can answer with the original outcome (idempotency).
 */
public record ExpeditedApproval(Instant approvedAt, int scoreAtApproval, List<String> warningsAtApproval) {

    public ExpeditedApproval {
        warningsAtApproval = List.copyOf(warningsAtApproval);
    }
}
