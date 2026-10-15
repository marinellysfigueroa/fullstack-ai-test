package com.insightdevelop.orders.domain;

/**
 * @param tier may be {@code null}; a null tier makes the order ineligible.
 */
public record Customer(String name, CustomerTier tier) {
}
