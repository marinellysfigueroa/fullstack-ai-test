package com.insightdevelop.orders.repository;

import com.insightdevelop.orders.domain.Customer;
import com.insightdevelop.orders.domain.CustomerTier;
import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.domain.OrderItem;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.domain.Shipping;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Seeds the in-memory store with orders that exercise every rule. Timestamps are relative
 * to startup so the demo data is fresh when the application starts.
 */
@Component
@ConditionalOnProperty(name = "orders.seed-sample-data", havingValue = "true", matchIfMissing = true)
public class SampleDataLoader implements ApplicationRunner {

    private final OrderRepository repository;
    private final Clock clock;

    public SampleDataLoader(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        InventoryStatus fresh = new InventoryStatus(true, now.minus(Duration.ofMinutes(1)));
        Instant recent = now.minus(Duration.ofHours(2));
        Shipping validUs = new Shipping("US", true);

        List.of(
                // Clean order: score 100, eligible.
                order("ORD-1001", "Acme Corp", CustomerTier.PLATINUM, List.of(item("SKU-1", 2, "120.00")), "240.00",
                        validUs, List.of(), fresh, OrderStatus.NEW, recent),
                // Quantity 4: score 70, eligible but risky.
                order("ORD-1002", "Globex", CustomerTier.GOLD, List.of(item("SKU-2", 4, "50.00")), "200.00",
                        validUs, List.of(), fresh, OrderStatus.NEW, recent),
                // Inventory unavailable: hard failure.
                order("ORD-1003", "Initech", CustomerTier.GOLD, List.of(item("SKU-3", 1, "80.00")), "80.00",
                        validUs, List.of(), new InventoryStatus(false, now.minus(Duration.ofMinutes(2))),
                        OrderStatus.NEW, recent),
                // Inventory checked 25 minutes ago: hard failure.
                order("ORD-1004", "Umbrella", CustomerTier.STANDARD, List.of(item("SKU-4", 1, "40.00")), "40.00",
                        validUs, List.of(), new InventoryStatus(true, now.minus(Duration.ofMinutes(25))),
                        OrderStatus.REVIEW, recent),
                // Customer without tier: hard failure.
                order("ORD-1005", "Hooli", null, List.of(item("SKU-5", 1, "99.00")), "99.00",
                        validUs, List.of(), fresh, OrderStatus.NEW, recent),
                // No items: hard failure.
                order("ORD-1006", "Vandelay", CustomerTier.STANDARD, List.of(), "0.00",
                        validUs, List.of(), fresh, OrderStatus.NEW, recent),
                // HIGH_VALUE, over GOLD threshold, address failed: 100-25-20-15 = 40 -> below minimum.
                order("ORD-1007", "Stark Industries", CustomerTier.GOLD, List.of(item("SKU-7", 2, "900.00")), "1800.00",
                        new Shipping("US", false), List.of("HIGH_VALUE"), fresh, OrderStatus.REVIEW, recent),
                // Inconsistent total, missing risk flags, address not validated: eligible with warnings.
                order("ORD-1008", "Wayne Enterprises", CustomerTier.PLATINUM,
                        List.of(item("SKU-8", 1, "300.00"), item("SKU-9", 1, "150.00")), "400.00",
                        new Shipping("CA", null), null, fresh, OrderStatus.NEW, recent),
                // Already approved: approving again is a no-op.
                order("ORD-1009", "Soylent", CustomerTier.GOLD, List.of(item("SKU-10", 1, "60.00")), "60.00",
                        validUs, List.of(), fresh, OrderStatus.APPROVED, recent),
                // 3 days old and quantity 4: 100-30-10 = 60, exactly at the minimum.
                order("ORD-1010", "Tyrell Corp", CustomerTier.STANDARD, List.of(item("SKU-11", 4, "25.00")), "100.00",
                        validUs, List.of(), fresh, OrderStatus.NEW, now.minus(Duration.ofDays(3))),
                // Rejected order.
                order("ORD-1011", "Cyberdyne", CustomerTier.PLATINUM, List.of(item("SKU-12", 1, "500.00")), "500.00",
                        validUs, List.of(), fresh, OrderStatus.REJECTED, recent),
                // Fraud suspected: hard failure.
                order("ORD-1012", "Oceanic Airlines", CustomerTier.GOLD, List.of(item("SKU-13", 1, "70.00")), "70.00",
                        validUs, List.of("FRAUD_SUSPECTED"), fresh, OrderStatus.REVIEW, recent),
                // Inventory never checked: hard failure.
                order("ORD-1013", "Massive Dynamic", CustomerTier.PLATINUM, List.of(item("SKU-14", 1, "45.00")), "45.00",
                        validUs, List.of(), new InventoryStatus(true, null), OrderStatus.NEW, recent)
        ).forEach(repository::save);
    }

    private static Order order(String id, String customer, CustomerTier tier, List<OrderItem> items, String total,
                               Shipping shipping, List<String> riskFlags, InventoryStatus inventory,
                               OrderStatus status, Instant createdAt) {
        return new Order(id, new Customer(customer, tier), items, new BigDecimal(total), shipping, riskFlags,
                inventory, status, createdAt, null, 0);
    }

    private static OrderItem item(String sku, int quantity, String unitPrice) {
        return new OrderItem(sku, "Product " + sku, quantity, new BigDecimal(unitPrice));
    }
}
