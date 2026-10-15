package com.insightdevelop.orders.api;

import com.insightdevelop.orders.api.dto.ApprovalResponse;
import com.insightdevelop.orders.api.dto.EligibilityResponse;
import com.insightdevelop.orders.api.dto.OrderResponse;
import com.insightdevelop.orders.api.dto.RefreshInventoryRequest;
import com.insightdevelop.orders.api.dto.ApiError;
import com.insightdevelop.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HTTP layer only: maps requests to {@link OrderService} calls and domain objects to DTOs. */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order review and expedited approval")
@ApiResponse(responseCode = "404", description = "Order not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List orders", description = "All orders, each with its current eligibility evaluation.")
    @ApiResponse(responseCode = "200", description = "Orders with eligibility")
    public List<OrderResponse> list() {
        return orderService.findAll().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order", description = "The order with its current eligibility evaluation.")
    @ApiResponse(responseCode = "200", description = "Order with eligibility")
    public OrderResponse get(@Parameter(description = "Order id", example = "ORD-1001") @PathVariable String id) {
        return OrderResponse.from(orderService.findById(id));
    }

    @GetMapping("/{id}/eligibility")
    @Operation(summary = "Evaluate eligibility",
            description = "Returns eligible, score (0-100), reasons and warnings, plus the decision and per-rule findings.")
    @ApiResponse(responseCode = "200", description = "Eligibility evaluation")
    public EligibilityResponse eligibility(
            @Parameter(description = "Order id", example = "ORD-1002") @PathVariable String id) {
        return EligibilityResponse.from(orderService.findById(id).eligibility());
    }

    @PostMapping("/{id}/approve-expedited")
    @Operation(summary = "Approve for expedited processing",
            description = """
                    Re-evaluates eligibility before approving. Idempotent: approving an already approved order
                    returns 200 with alreadyApproved=true. Concurrent requests for the same order are serialized,
                    so exactly one performs the approval.""")
    @ApiResponse(responseCode = "200", description = "Approved, or already approved (alreadyApproved=true)")
    @ApiResponse(responseCode = "409", description = "INVALID_STATUS: the order is REJECTED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "NOT_ELIGIBLE: re-evaluation failed; reasons explain why",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ApprovalResponse approveExpedited(
            @Parameter(description = "Order id", example = "ORD-1001") @PathVariable String id) {
        return ApprovalResponse.from(orderService.approveExpedited(id));
    }

    /** Demo helper standing in for the inventory service: records a fresh inventory check. */
    @PostMapping("/{id}/inventory/refresh")
    @Operation(summary = "Simulate an inventory check (demo)",
            description = "Sets inventoryStatus.lastCheckedAt to now; optionally changes availability.")
    @ApiResponse(responseCode = "200", description = "Order with updated eligibility")
    public OrderResponse refreshInventory(
            @Parameter(description = "Order id", example = "ORD-1004") @PathVariable String id,
                                          @RequestBody(required = false) RefreshInventoryRequest request) {
        return OrderResponse.from(orderService.refreshInventory(id, request == null ? null : request.available()));
    }
}
