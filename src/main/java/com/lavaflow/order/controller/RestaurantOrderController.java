package com.lavaflow.order.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.OrderSummaryResponse;
import com.lavaflow.order.service.OrderQueryService;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/me/orders")
@PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
@RequiredArgsConstructor
@Tag(name = "Restaurant Orders", description = "Orders received by the authenticated restaurant")
public class RestaurantOrderController {

    private final OrderQueryService orderQueryService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @GetMapping
    @Operation(summary = "List my restaurant's orders, filtered by outlet or status")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        PageResponse<OrderSummaryResponse> page = PageResponse.from(
                orderQueryService.getRestaurantOrders(restaurantId, outletId, status, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success("Orders fetched.", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order of my restaurant, including customer name and phone")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Order fetched.",
                orderQueryService.getRestaurantOrder(restaurantId, id)
        ));
    }
}
