package com.insightdevelop.orders.api;

import com.insightdevelop.orders.api.dto.ApprovalResponse;
import com.insightdevelop.orders.api.dto.EligibilityResponse;
import com.insightdevelop.orders.api.dto.OrderResponse;
import com.insightdevelop.orders.api.dto.RefreshInventoryRequest;
import com.insightdevelop.orders.service.OrderService;
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
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.findAll().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orderService.findById(id));
    }

    @GetMapping("/{id}/eligibility")
    public EligibilityResponse eligibility(@PathVariable String id) {
        return EligibilityResponse.from(orderService.findById(id).eligibility());
    }

    @PostMapping("/{id}/approve-expedited")
    public ApprovalResponse approveExpedited(@PathVariable String id) {
        return ApprovalResponse.from(orderService.approveExpedited(id));
    }

    /** Demo helper standing in for the inventory service: records a fresh inventory check. */
    @PostMapping("/{id}/inventory/refresh")
    public OrderResponse refreshInventory(@PathVariable String id,
                                          @RequestBody(required = false) RefreshInventoryRequest request) {
        return OrderResponse.from(orderService.refreshInventory(id, request == null ? null : request.available()));
    }
}
