package com.insightdevelop.orders.eligibility.rule.hard;

import com.insightdevelop.orders.domain.OrderStatus;
import com.insightdevelop.orders.eligibility.EvaluationContext;
import com.insightdevelop.orders.eligibility.rule.HardRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Hard rule (carried over from the previous version): rejected orders cannot be expedited. */
@Component
public class OrderStatusRule implements HardRule {

    @Override
    public String id() {
        return "ORDER_STATUS";
    }

    @Override
    public Optional<String> violation(EvaluationContext context) {
        return context.order().status() == OrderStatus.REJECTED
                ? Optional.of("Order has been rejected")
                : Optional.empty();
    }
}
