package com.insightdevelop.orders.eligibility;

import com.insightdevelop.orders.domain.Customer;
import com.insightdevelop.orders.domain.CustomerTier;
import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.OrderItem;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.domain.Shipping;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.insightdevelop.orders.TestOrders.CLOCK;
import static com.insightdevelop.orders.TestOrders.NOW;
import static com.insightdevelop.orders.TestOrders.PROPERTIES;
import static com.insightdevelop.orders.TestOrders.allRules;
import static com.insightdevelop.orders.TestOrders.order;
import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEngineTest {

    private final EligibilityEngine engine = new EligibilityEngine(allRules(), PROPERTIES, CLOCK);

    private static List<String> blockingRuleIds(EligibilityResult result) {
        return result.findings().stream().filter(f -> f.severity() == Severity.BLOCKING).map(Finding::ruleId).toList();
    }

    @Test
    void cleanOrderIsFullyEligible() {
        EligibilityResult result = engine.evaluate(order().build());

        assertThat(result.eligible()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.decision()).isEqualTo(Decision.ELIGIBLE);
        assertThat(result.reasons()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Nested
    class HardRules {

        @Test
        void inventoryUnavailable() {
            var result = engine.evaluate(order().inventory(new InventoryStatus(false, NOW)).build());
            assertThat(result.eligible()).isFalse();
            assertThat(blockingRuleIds(result)).containsExactly("INVENTORY_AVAILABLE");
            assertThat(result.score()).isEqualTo(100); // hard rules block regardless of score
        }

        @Test
        void inventoryOlderThanTenMinutes() {
            var result = engine.evaluate(order()
                    .inventory(new InventoryStatus(true, NOW.minus(Duration.ofMinutes(10)).minusSeconds(1))).build());
            assertThat(blockingRuleIds(result)).containsExactly("INVENTORY_FRESH");
        }

        @Test
        void inventoryExactlyTenMinutesOldIsStillFresh() {
            var result = engine.evaluate(order()
                    .inventory(new InventoryStatus(true, NOW.minus(Duration.ofMinutes(10)))).build());
            assertThat(result.eligible()).isTrue();
        }

        @Test
        void inventoryNeverChecked() {
            var result = engine.evaluate(order().inventory(new InventoryStatus(true, null)).build());
            assertThat(blockingRuleIds(result)).containsExactly("INVENTORY_FRESH");
        }

        @Test
        void inventoryMissing() {
            var result = engine.evaluate(order().inventory(null).build());
            assertThat(blockingRuleIds(result)).containsExactly("INVENTORY_AVAILABLE");
        }

        @Test
        void tierNull() {
            var result = engine.evaluate(order().tier(null).build());
            assertThat(blockingRuleIds(result)).containsExactly("CUSTOMER_TIER");
        }

        @Test
        void customerMissing() {
            var result = engine.evaluate(order().customer(null).build());
            assertThat(blockingRuleIds(result)).containsExactly("CUSTOMER_TIER");
        }

        @Test
        void itemsEmptyOrNull() {
            assertThat(blockingRuleIds(engine.evaluate(order().items(List.of()).totalAmount("0").build())))
                    .containsExactly("ITEMS_PRESENT");
            assertThat(blockingRuleIds(engine.evaluate(order().items(null).totalAmount("0").build())))
                    .containsExactly("ITEMS_PRESENT");
        }

        @Test
        void unsupportedCountryOrMissingShipping() {
            assertThat(blockingRuleIds(engine.evaluate(order().shipping(new Shipping("BR", true)).build())))
                    .containsExactly("SHIPPING_DESTINATION");
            assertThat(blockingRuleIds(engine.evaluate(order().shipping(null).build())))
                    .containsExactly("SHIPPING_DESTINATION");
        }

        @Test
        void blockingRiskFlagAndRejectedStatus() {
            assertThat(blockingRuleIds(engine.evaluate(order().riskFlags(List.of("FRAUD_SUSPECTED")).build())))
                    .containsExactly("BLOCKING_RISK_FLAG");
            assertThat(blockingRuleIds(engine.evaluate(order().status(OrderStatus.REJECTED).build())))
                    .containsExactly("ORDER_STATUS");
        }

        @Test
        void multipleHardFailuresAreAllReported() {
            var result = engine.evaluate(order().tier(null).items(List.of()).totalAmount("0")
                    .inventory(new InventoryStatus(false, null)).build());
            assertThat(blockingRuleIds(result))
                    .containsExactlyInAnyOrder("CUSTOMER_TIER", "ITEMS_PRESENT", "INVENTORY_AVAILABLE", "INVENTORY_FRESH");
            assertThat(result.reasons()).hasSize(4);
        }
    }

    @Nested
    class Scoring {

        @Test
        void quantityAboveThreeCosts30() {
            assertThat(engine.evaluate(order().quantity(3).build()).score()).isEqualTo(100);
            var result = engine.evaluate(order().quantity(4).build());
            assertThat(result.score()).isEqualTo(70);
            assertThat(result.eligible()).isTrue();
            assertThat(result.decision()).isEqualTo(Decision.ELIGIBLE_WITH_RISK);
            assertThat(result.reasons()).singleElement().asString().contains("-30");
        }

        @Test
        void amountAboveTierThresholdCosts20() {
            var gold = order().tier(CustomerTier.GOLD)
                    .items(List.of(new OrderItem("S", "x", 1, new BigDecimal("1500.01"))))
                    .totalAmount("1500.01").build();
            assertThat(engine.evaluate(gold).score()).isEqualTo(80);

            var platinum = order().tier(CustomerTier.PLATINUM)
                    .items(List.of(new OrderItem("S", "x", 1, new BigDecimal("1500.01"))))
                    .totalAmount("1500.01").build();
            assertThat(engine.evaluate(platinum).score()).isEqualTo(100);
        }

        @Test
        void addressValidationFailedCosts15ButUnknownIsOnlyAWarning() {
            var failed = engine.evaluate(order().shipping(new Shipping("US", false)).build());
            assertThat(failed.score()).isEqualTo(85);

            var unknown = engine.evaluate(order().shipping(new Shipping("US", null)).build());
            assertThat(unknown.score()).isEqualTo(100);
            assertThat(unknown.warnings()).hasSize(1);
            assertThat(unknown.decision()).isEqualTo(Decision.ELIGIBLE_WITH_RISK);
        }

        @Test
        void highValueFlagCosts25() {
            assertThat(engine.evaluate(order().riskFlags(List.of("HIGH_VALUE")).build()).score()).isEqualTo(75);
        }

        @Test
        void ordersOlderThan48HoursCost10() {
            assertThat(engine.evaluate(order().createdAt(NOW.minus(Duration.ofHours(48))).build()).score()).isEqualTo(100);
            assertThat(engine.evaluate(order().createdAt(NOW.minus(Duration.ofHours(49))).build()).score()).isEqualTo(90);
        }

        @Test
        void scoreOfExactly60IsEligible() {
            var result = engine.evaluate(order().quantity(4).createdAt(NOW.minus(Duration.ofDays(3))).build());
            assertThat(result.score()).isEqualTo(60);
            assertThat(result.eligible()).isTrue();
            assertThat(result.decision()).isEqualTo(Decision.ELIGIBLE_WITH_RISK);
        }

        @Test
        void scoreBelow60IsNotEligible() {
            var result = engine.evaluate(order().quantity(4).riskFlags(List.of("HIGH_VALUE")).build());
            assertThat(result.score()).isEqualTo(45);
            assertThat(result.eligible()).isFalse();
            assertThat(result.decision()).isEqualTo(Decision.NOT_ELIGIBLE);
            assertThat(blockingRuleIds(result)).containsExactly("MINIMUM_SCORE");
            assertThat(result.reasons()).hasSize(3); // minimum score + two deductions
        }

        @Test
        void scoreNeverDropsBelowZero() {
            var result = engine.evaluate(order().quantity(400)
                    .riskFlags(List.of("HIGH_VALUE")).shipping(new Shipping("US", false))
                    .createdAt(NOW.minus(Duration.ofDays(5))).build());
            assertThat(result.score()).isEqualTo(0);
        }
    }

    @Nested
    class MissingOrInconsistentData {

        @Test
        void inconsistentTotalUsesHigherValueAndWarns() {
            // items total 1600 > GOLD threshold although declared total is 100
            var result = engine.evaluate(order().tier(CustomerTier.GOLD)
                    .items(List.of(new OrderItem("S", "x", 2, new BigDecimal("800.00"))))
                    .totalAmount("100.00").build());
            assertThat(result.score()).isEqualTo(80);
            assertThat(result.warnings()).singleElement().asString().contains("does not match");
        }

        @Test
        void missingTotalUsesItemsTotal() {
            var result = engine.evaluate(order().totalAmount(null).build());
            assertThat(result.eligible()).isTrue();
            assertThat(result.warnings()).singleElement().asString().contains("totalAmount is missing");
        }

        @Test
        void missingRiskFlagsAreTreatedAsEmptyWithWarning() {
            var result = engine.evaluate(order().riskFlags(null).build());
            assertThat(result.eligible()).isTrue();
            assertThat(result.score()).isEqualTo(100);
            assertThat(result.warnings()).singleElement().asString().contains("Risk assessment is missing");
        }

        @Test
        void invalidItemQuantityWarns() {
            var result = engine.evaluate(order().items(List.of(
                    new OrderItem("A", "a", 1, new BigDecimal("10")),
                    new OrderItem("B", "b", null, new BigDecimal("10")))).totalAmount("10").build());
            assertThat(result.warnings()).anyMatch(w -> w.contains("Item B has an invalid quantity"));
        }

        @Test
        void missingOrFutureCreatedAtWarns() {
            assertThat(engine.evaluate(order().createdAt(null).build()).warnings()).hasSize(1);
            assertThat(engine.evaluate(order().createdAt(NOW.plusSeconds(60)).build()).warnings()).hasSize(1);
        }

        @Test
        void unknownTierDoesNotAlsoFailThresholdRule() {
            var result = engine.evaluate(order().customer(new Customer("x", null)).build());
            assertThat(result.reasons()).hasSize(1);
        }
    }

    @Test
    void newRulesPlugInWithoutTouchingExistingOnes() {
        HardRule weekendFreeze = new HardRule() {
            @Override
            public String id() {
                return "TEST_FREEZE";
            }

            @Override
            public Optional<String> violation(EvaluationContext context) {
                return Optional.of("Expedited processing frozen");
            }
        };
        List<EligibilityRule> rules = allRules();
        rules.add(weekendFreeze);

        var result = new EligibilityEngine(rules, PROPERTIES, CLOCK).evaluate(order().build());

        assertThat(result.eligible()).isFalse();
        assertThat(result.blockingReasons()).containsExactly("Expedited processing frozen");
    }
}
