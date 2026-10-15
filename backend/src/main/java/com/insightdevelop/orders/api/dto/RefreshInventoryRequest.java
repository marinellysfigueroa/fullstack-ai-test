package com.insightdevelop.orders.api.dto;

/** @param available new availability; {@code null} keeps the current value and only refreshes the timestamp */
public record RefreshInventoryRequest(Boolean available) {
}
