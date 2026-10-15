package com.insightdevelop.orders;

import com.insightdevelop.orders.config.EligibilityProperties;
import com.insightdevelop.orders.domain.Customer;
import com.insightdevelop.orders.domain.CustomerTier;
import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.domain.OrderItem;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import com.insightdevelop.orders.eligibility.rule.hard.BlockingRiskFlagRule;
import com.insightdevelop.orders.eligibility.rule.hard.CustomerTierRule;
import com.insightdevelop.orders.eligibility.rule.hard.InventoryAvailableRule;
import com.insightdevelop.orders.eligibility.rule.hard.InventoryFreshnessRule;
import com.insightdevelop.orders.eligibility.rule.hard.ItemsPresentRule;
import com.insightdevelop.orders.eligibility.rule.hard.OrderStatusRule;
import com.insightdevelop.orders.eligibility.rule.hard.ShippingDestinationRule;
import com.insightdevelop.orders.eligibility.rule.quality.AddressValidationPendingRule;
import com.insightdevelop.orders.eligibility.rule.quality.ItemDataQualityRule;
import com.insightdevelop.orders.eligibility.rule.quality.MissingRiskAssessmentRule;
import com.insightdevelop.orders.eligibility.rule.quality.TimestampSanityRule;
import com.insightdevelop.orders.eligibility.rule.quality.TotalAmountConsistencyRule;
import com.insightdevelop.orders.eligibility.rule.score.AddressNotValidatedRule;
import com.insightdevelop.orders.eligibility.rule.score.HighQuantityRule;
import com.insightdevelop.orders.eligibility.rule.score.HighValueRiskFlagRule;
import com.insightdevelop.orders.eligibility.rule.score.OrderAgeRule;
import com.insightdevelop.orders.eligibility.rule.score.TierAmountThresholdRule;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared fixtures: a fixed clock, default properties, all rules, and a builder for a clean order. */
public final class TestOrders {

    public static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    public static final EligibilityProperties PROPERTIES = new EligibilityProperties(
            60, 80, Duration.ofMinutes(10), Duration.ofHours(48), 3,
            Map.of(CustomerTier.STANDARD, new BigDecimal("500"),
                    CustomerTier.GOLD, new BigDecimal("1500"),
                    CustomerTier.PLATINUM, new BigDecimal("5000")),
            Set.of("US", "CA"), Set.of("FRAUD_SUSPECTED"), new BigDecimal("0.01"));

    private TestOrders() {
    }

    public static List<EligibilityRule> allRules() {
        return new ArrayList<>(List.of(
                new InventoryAvailableRule(), new InventoryFreshnessRule(), new CustomerTierRule(),
                new ItemsPresentRule(), new ShippingDestinationRule(), new BlockingRiskFlagRule(),
                new OrderStatusRule(),
                new HighQuantityRule(), new TierAmountThresholdRule(), new AddressNotValidatedRule(),
                new HighValueRiskFlagRule(), new OrderAgeRule(),
                new TotalAmountConsistencyRule(), new ItemDataQualityRule(), new MissingRiskAssessmentRule(),
                new AddressValidationPendingRule(), new TimestampSanityRule()));
    }

    public static Builder order() {
        return new Builder();
    }

    public static final class Builder {
        private String id = "ORD-1";
        private Customer customer = new Customer("Acme", CustomerTier.GOLD);
        private List<OrderItem> items = List.of(new OrderItem("SKU-1", "Widget", 1, new BigDecimal("100.00")));
        private BigDecimal totalAmount = new BigDecimal("100.00");
        private Shipping shipping = new Shipping("US", true);
        private List<String> riskFlags = List.of();
        private InventoryStatus inventory = new InventoryStatus(true, NOW.minus(Duration.ofMinutes(1)));
        private OrderStatus status = OrderStatus.NEW;
        private Instant createdAt = NOW.minus(Duration.ofHours(1));

        public Builder id(String v) { id = v; return this; }
        public Builder customer(Customer v) { customer = v; return this; }
        public Builder tier(CustomerTier v) { customer = new Customer("Acme", v); return this; }
        public Builder items(List<OrderItem> v) { items = v; return this; }
        public Builder quantity(int q) {
            items = List.of(new OrderItem("SKU-1", "Widget", q, new BigDecimal("10.00")));
            totalAmount = new BigDecimal("10.00").multiply(BigDecimal.valueOf(q));
            return this;
        }
        public Builder totalAmount(String v) { totalAmount = v == null ? null : new BigDecimal(v); return this; }
        public Builder shipping(Shipping v) { shipping = v; return this; }
        public Builder riskFlags(List<String> v) { riskFlags = v; return this; }
        public Builder inventory(InventoryStatus v) { inventory = v; return this; }
        public Builder status(OrderStatus v) { status = v; return this; }
        public Builder createdAt(Instant v) { createdAt = v; return this; }

        public Order build() {
            return new Order(id, customer, items, totalAmount, shipping, riskFlags, inventory, status, createdAt, null, 0);
        }
    }
}
