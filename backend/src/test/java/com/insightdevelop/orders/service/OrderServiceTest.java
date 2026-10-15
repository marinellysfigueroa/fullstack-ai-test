package com.insightdevelop.orders.service;

import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.eligibility.EligibilityEngine;
import com.insightdevelop.orders.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.insightdevelop.orders.TestOrders.CLOCK;
import static com.insightdevelop.orders.TestOrders.NOW;
import static com.insightdevelop.orders.TestOrders.PROPERTIES;
import static com.insightdevelop.orders.TestOrders.allRules;
import static com.insightdevelop.orders.TestOrders.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final OrderService service =
            new OrderService(repository, new EligibilityEngine(allRules(), PROPERTIES, CLOCK), CLOCK);

    @Test
    void approvesEligibleOrder() {
        repository.save(order().id("A").build());

        ApprovalOutcome outcome = service.approveExpedited("A");

        assertThat(outcome.alreadyApproved()).isFalse();
        assertThat(outcome.order().status()).isEqualTo(OrderStatus.APPROVED);
        assertThat(outcome.order().expeditedApproval().approvedAt()).isEqualTo(NOW);
        assertThat(outcome.order().expeditedApproval().scoreAtApproval()).isEqualTo(100);
        assertThat(repository.findById("A").orElseThrow().status()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    void secondApprovalIsIdempotent() {
        repository.save(order().id("A").build());
        ApprovalOutcome first = service.approveExpedited("A");

        // Even if the order would be ineligible now, the earlier approval stands.
        repository.save(first.order().withInventoryStatus(new InventoryStatus(false, NOW)));
        ApprovalOutcome second = service.approveExpedited("A");

        assertThat(second.alreadyApproved()).isTrue();
        assertThat(second.order().status()).isEqualTo(OrderStatus.APPROVED);
        assertThat(second.order().expeditedApproval()).isEqualTo(first.order().expeditedApproval());
    }

    @Test
    void orderThatBecameIneligibleIsRefusedWithReasons() {
        repository.save(order().id("A").inventory(new InventoryStatus(true, NOW.minus(Duration.ofMinutes(30)))).build());

        assertThatThrownBy(() -> service.approveExpedited("A"))
                .isInstanceOfSatisfying(ApprovalRejectedException.class, ex -> {
                    assertThat(ex.reason()).isEqualTo(ApprovalRejectedException.Reason.NOT_ELIGIBLE);
                    assertThat(ex.eligibility().reasons()).singleElement().asString().contains("stale");
                });
        assertThat(repository.findById("A").orElseThrow().status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void rejectedOrderCannotBeApproved() {
        repository.save(order().id("A").status(OrderStatus.REJECTED).build());

        assertThatThrownBy(() -> service.approveExpedited("A"))
                .isInstanceOfSatisfying(ApprovalRejectedException.class,
                        ex -> assertThat(ex.reason()).isEqualTo(ApprovalRejectedException.Reason.INVALID_STATUS));
    }

    @Test
    void unknownOrder() {
        assertThatThrownBy(() -> service.approveExpedited("NOPE")).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void concurrentApprovalsTransitionExactlyOnce() throws Exception {
        repository.save(order().id("A").build());
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ApprovalOutcome>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<ApprovalOutcome> call = () -> {
                    start.await();
                    return service.approveExpedited("A");
                };
                futures.add(pool.submit(call));
            }
            start.countDown();

            int fresh = 0;
            for (Future<ApprovalOutcome> f : futures) {
                if (!f.get().alreadyApproved()) {
                    fresh++;
                }
            }
            assertThat(fresh).isEqualTo(1);
            assertThat(repository.findById("A").orElseThrow().version()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void canApproveFlagIsComputedByBackend() {
        repository.save(order().id("A").build());
        repository.save(order().id("B").inventory(new InventoryStatus(false, NOW)).build());

        assertThat(service.findById("A").canApproveExpedited()).isTrue();
        assertThat(service.findById("B").canApproveExpedited()).isFalse();
        service.approveExpedited("A");
        assertThat(service.findById("A").canApproveExpedited()).isFalse();
    }

    @Test
    void refreshInventoryMakesStaleOrderEligibleAgain() {
        repository.save(order().id("A").inventory(new InventoryStatus(true, NOW.minus(Duration.ofHours(1)))).build());
        assertThat(service.findById("A").eligibility().eligible()).isFalse();

        assertThat(service.refreshInventory("A", null).eligibility().eligible()).isTrue();
    }
}
