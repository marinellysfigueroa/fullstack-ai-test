package com.insightdevelop.orders.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Structured error body shared by all endpoints.
 *
 * @param code machine-readable code, e.g. ORDER_NOT_FOUND, NOT_ELIGIBLE, INVALID_STATUS
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String code,
        String message,
        String orderId,
        List<String> reasons,
        List<String> warnings,
        EligibilityResponse eligibility,
        Instant timestamp) {
}
