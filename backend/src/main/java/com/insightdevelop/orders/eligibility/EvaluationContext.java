package com.insightdevelop.orders.eligibility;

import com.insightdevelop.orders.config.EligibilityProperties;
import com.insightdevelop.orders.domain.CustomerTier;
import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.domain.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything a rule needs: the raw order, the evaluation instant and the configuration,
 * plus null-safe accessors so individual rules do not repeat defensive code.
 */
public record EvaluationContext(Order order, Instant now, EligibilityProperties properties) {

    public List<OrderItem> items() {
        return order.items() == null ? List.of() : order.items().stream().filter(Objects::nonNull).toList();
    }

    public List<String> riskFlags() {
        return order.riskFlags() == null ? List.of() : order.riskFlags();
    }

    public Optional<CustomerTier> tier() {
        return Optional.ofNullable(order.customer()).map(c -> c.tier());
    }

    /** Sum of quantities; missing or non-positive quantities count as zero. */
    public int totalQuantity() {
        return items().stream()
                .map(OrderItem::quantity)
                .filter(q -> q != null && q > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /** Sum of quantity * unitPrice over items with complete pricing data. */
    public BigDecimal itemsTotal() {
        return items().stream()
                .filter(i -> i.quantity() != null && i.quantity() > 0 && i.unitPrice() != null)
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Amount used for threshold checks. When the declared total and the item sum disagree,
     * the larger value is used (conservative: never under-estimate the order value).
     */
    public BigDecimal effectiveAmount() {
        BigDecimal declared = order.totalAmount();
        BigDecimal computed = itemsTotal();
        return declared == null ? computed : declared.max(computed);
    }
}
