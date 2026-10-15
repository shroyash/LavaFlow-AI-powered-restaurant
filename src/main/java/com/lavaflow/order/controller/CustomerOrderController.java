package com.lavaflow.order.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.OrderSummaryResponse;
import com.lavaflow.order.dto.PlaceOrderRequest;
import com.lavaflow.order.dto.PlaceOrderResult;
import com.lavaflow.order.service.OrderCancellationService;
import com.lavaflow.order.service.OrderQueryService;
import com.lavaflow.order.service.OrderSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orders of the logged-in customer")
public class CustomerOrderController {

    private final OrderSubmissionService orderSubmissionService;
    private final OrderQueryService orderQueryService;
    private final OrderCancellationService orderCancellationService;

    @PostMapping
    @Operation(summary = "Place an order; retrying with the same Idempotency-Key returns the original order")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        PlaceOrderResult result = orderSubmissionService.submit(principal.getUser().getId(), idempotencyKey, request);

        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = result.replayed() ? "Order was already placed." : "Order placed.";

        return ResponseEntity.status(status).body(ApiResponse.success(message, result.order()));
    }

    @GetMapping
    @Operation(summary = "List my orders, newest first, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<OrderSummaryResponse> page = PageResponse.from(
                orderQueryService.getCustomerOrders(principal.getUser().getId(), status, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success("Orders fetched.", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my orders")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order fetched.",
                orderQueryService.getCustomerOrder(principal.getUser().getId(), id)
        ));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel my order while the restaurant has not started on it")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order cancelled.",
                orderCancellationService.cancelByCustomer(principal.getUser().getId(), id)
        ));
    }
}