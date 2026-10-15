package com.insightdevelop.orders.repository;

import com.insightdevelop.orders.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe in-memory store. Orders are immutable records, so reads never see partial updates. */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();

    @Override
    public List<Order> findAll() {
        return orders.values().stream().sorted(Comparator.comparing(Order::orderId)).toList();
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public Order save(Order order) {
        orders.put(order.orderId(), order);
        return order;
    }
}
