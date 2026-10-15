package com.insightdevelop.orders.api;

import com.insightdevelop.orders.api.dto.ApiError;
import com.insightdevelop.orders.api.dto.EligibilityResponse;
import com.insightdevelop.orders.service.ApprovalRejectedException;
import com.insightdevelop.orders.service.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> notFound(OrderNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), ex.orderId(), List.of(ex.getMessage()),
                null, null);
    }

    /**
     * 409 for a status that forbids approval, 422 when re-evaluation finds the order ineligible.
     * Both carry the reasons so the UI can show exactly why the approval failed.
     */
    @ExceptionHandler(ApprovalRejectedException.class)
    public ResponseEntity<ApiError> approvalRejected(ApprovalRejectedException ex) {
        EligibilityResponse eligibility = EligibilityResponse.from(ex.eligibility());
        return switch (ex.reason()) {
            case INVALID_STATUS -> build(HttpStatus.CONFLICT, "INVALID_STATUS", ex.getMessage(), ex.orderId(),
                    List.of(ex.getMessage()), eligibility.warnings(), eligibility);
            case NOT_ELIGIBLE -> build(HttpStatus.UNPROCESSABLE_ENTITY, "NOT_ELIGIBLE", ex.getMessage(), ex.orderId(),
                    eligibility.reasons(), eligibility.warnings(), eligibility);
        };
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, String orderId,
                                           List<String> reasons, List<String> warnings, EligibilityResponse eligibility) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), code, message, orderId, reasons, warnings, eligibility, clock.instant()));
    }
}
