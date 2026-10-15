package com.insightdevelop.orders.domain;

import java.math.BigDecimal;

/**
 * Line item. {@code quantity} and {@code unitPrice} may be missing in source data;
 * the eligibility rules decide how to treat that.
 */
public record OrderItem(String sku, String name, Integer quantity, BigDecimal unitPrice) {
}
