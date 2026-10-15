package com.insightdevelop.orders.repository;

import com.insightdevelop.orders.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    List<Order> findAll();

    Optional<Order> findById(String orderId);

    Order save(Order order);
}
