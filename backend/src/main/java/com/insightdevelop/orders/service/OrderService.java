package com.insightdevelop.orders.service;

import com.insightdevelop.orders.domain.ExpeditedApproval;
import com.insightdevelop.orders.domain.InventoryStatus;
import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.eligibility.EligibilityEngine;
import com.insightdevelop.orders.eligibility.EligibilityResult;
import com.insightdevelop.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Application service: reads orders, evaluates them, and performs the expedited approval.
 * <p>
 * Eligibility is always computed on read because several rules depend on the current time.
 */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final EligibilityEngine engine;
    private final Clock clock;
    /** One monitor per order: concurrent writes to the same order are serialized, different orders run in parallel. */
    private final ConcurrentMap<String, Object> orderLocks = new ConcurrentHashMap<>();

    public OrderService(OrderRepository repository, EligibilityEngine engine, Clock clock) {
        this.repository = repository;
        this.engine = engine;
        this.clock = clock;
    }

    public List<OrderEvaluation> findAll() {
        return repository.findAll().stream().map(this::evaluate).toList();
    }

    public OrderEvaluation findById(String orderId) {
        return evaluate(load(orderId));
    }

    /**
     * Approves an order for expedited processing.
     * <ul>
     *   <li>Idempotent: approving an already approved order returns the original approval.</li>
     *   <li>Eligibility is re-evaluated at approval time; an order that became ineligible is refused.</li>
     *   <li>Concurrent requests for the same order are serialized; exactly one performs the transition.</li>
     * </ul>
     */
    public ApprovalOutcome approveExpedited(String orderId) {
        synchronized (lockFor(load(orderId).orderId())) {
            Order order = load(orderId);

            if (order.status() == OrderStatus.APPROVED) {
                return new ApprovalOutcome(order, true, null);
            }
            EligibilityResult eligibility = engine.evaluate(order);
            if (order.status() == OrderStatus.REJECTED) {
                throw new ApprovalRejectedException(orderId, ApprovalRejectedException.Reason.INVALID_STATUS,
                        "Order " + orderId + " is REJECTED and cannot be approved", eligibility);
            }
            if (!eligibility.eligible()) {
                throw new ApprovalRejectedException(orderId, ApprovalRejectedException.Reason.NOT_ELIGIBLE,
                        "Order " + orderId + " is not eligible for expedited processing", eligibility);
            }
            Order approved = order.approveExpedited(
                    new ExpeditedApproval(clock.instant(), eligibility.score(), eligibility.warnings()));
            repository.save(approved);
            return new ApprovalOutcome(approved, false, eligibility);
        }
    }

    /**
     * Simulates a fresh inventory check (stand-in for the inventory service) so the demo data
     * does not go stale after 10 minutes.
     */
    public OrderEvaluation refreshInventory(String orderId, Boolean available) {
        synchronized (lockFor(load(orderId).orderId())) {
            Order order = load(orderId);
            boolean newAvailability = available != null
                    ? available
                    : order.inventoryStatus() != null && order.inventoryStatus().available();
            Order updated = repository.save(order.withInventoryStatus(new InventoryStatus(newAvailability, clock.instant())));
            return evaluate(updated);
        }
    }

    private OrderEvaluation evaluate(Order order) {
        return new OrderEvaluation(order, engine.evaluate(order));
    }

    private Order load(String orderId) {
        return repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /** Only called for existing orders, so unknown ids cannot grow the lock map. */
    private Object lockFor(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new Object());
    }
}
