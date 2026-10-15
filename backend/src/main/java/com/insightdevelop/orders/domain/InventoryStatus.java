package com.insightdevelop.orders.domain;

import java.time.Instant;

/**
 * @param lastCheckedAt may be {@code null} when inventory was never checked.
 */
public record InventoryStatus(boolean available, Instant lastCheckedAt) {
}
