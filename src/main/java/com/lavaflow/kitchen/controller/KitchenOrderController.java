package com.lavaflow.kitchen.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.kitchen.dto.KitchenOrderResponse;
import com.lavaflow.kitchen.dto.RejectKitchenOrderRequest;
import com.lavaflow.kitchen.service.KitchenOrderActionService;
import com.lavaflow.kitchen.service.KitchenOrderService;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
@PreAuthorize("hasAnyRole('KITCHEN_STAFF', 'RESTAURANT_ADMIN', 'RESTAURANT_STAFF')")
@RequiredArgsConstructor
@Tag(name = "Kitchen", description = "Kitchen queue and order preparation for the authenticated restaurant")
public class KitchenOrderController {

    private final KitchenOrderService kitchenOrderService;
    private final KitchenOrderActionService kitchenOrderActionService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @GetMapping
    @Operation(summary = "Kitchen queue, oldest first; active orders only unless activeOnly=false")
    public ResponseEntity<ApiResponse<PageResponse<KitchenOrderResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) KitchenOrderStatus status,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        PageResponse<KitchenOrderResponse> page = PageResponse.from(
                kitchenOrderService.getKitchenOrders(restaurantId, outletId, status, activeOnly, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success("Kitchen orders fetched.", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a kitchen order")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Kitchen order fetched.",
                kitchenOrderService.getKitchenOrder(restaurantId, id)
        ));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a new order; reserved stock is deducted")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Order accepted.",
                kitchenOrderActionService.accept(restaurantId, id)
        ));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a new order with a reason; reserved stock is released")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RejectKitchenOrderRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Order rejected.",
                kitchenOrderActionService.reject(restaurantId, id, request.getReason())
        ));
    }

    @PostMapping("/{id}/preparing")
    @Operation(summary = "Start preparing an accepted order")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> startPreparing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Order is being prepared.",
                kitchenOrderActionService.startPreparing(restaurantId, id)
        ));
    }

    @PostMapping("/{id}/ready")
    @Operation(summary = "Mark an order ready for pickup by the rider")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> markReady(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Order is ready.",
                kitchenOrderActionService.markReady(restaurantId, id)
        ));
    }
}
