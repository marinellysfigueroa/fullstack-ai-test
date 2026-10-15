package com.insightdevelop.orders.domain;

/**
 * @param addressValidated {@code true}, {@code false} or {@code null} (validation not performed yet).
 */
public record Shipping(String country, Boolean addressValidated) {
}
