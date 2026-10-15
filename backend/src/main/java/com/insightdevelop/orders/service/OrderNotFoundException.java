package com.insightdevelop.orders.service;

public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order " + orderId + " was not found");
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
