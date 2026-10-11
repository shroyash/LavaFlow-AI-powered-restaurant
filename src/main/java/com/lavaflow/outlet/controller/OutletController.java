package com.lavaflow.outlet.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.outlet.dto.CreateOutletRequest;
import com.lavaflow.outlet.dto.OutletResponse;
import com.lavaflow.outlet.dto.UpdateOutletRequest;
import com.lavaflow.outlet.dto.UpdateOutletStatusRequest;
import com.lavaflow.outlet.service.OutletService;
import com.lavaflow.outlet.service.OutletStatusService;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outlets")
@RequiredArgsConstructor
@Tag(name = "Outlets", description = "Outlet management for the authenticated restaurant")
public class OutletController {

    private final OutletService outletService;
    private final OutletStatusService outletStatusService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Create an outlet")
    public ResponseEntity<ApiResponse<OutletResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOutletRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Outlet created.", outletService.createOutlet(restaurantId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "List outlets of the restaurant, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<OutletResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OutletStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        PageResponse<OutletResponse> page =
                PageResponse.from(outletService.getOutlets(restaurantId, status, pageable));
        return ResponseEntity.ok(ApiResponse.success("Outlets fetched.", page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Get an outlet by id")
    public ResponseEntity<ApiResponse<OutletResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success("Outlet fetched.", outletService.getOutlet(restaurantId, id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Update outlet details")
    public ResponseEntity<ApiResponse<OutletResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOutletRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success("Outlet updated.", outletService.updateOutlet(restaurantId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF')")
    @Operation(summary = "Change outlet status (ACTIVE, INACTIVE, BUSY)")
    public ResponseEntity<ApiResponse<OutletResponse>> changeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOutletStatusRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Outlet status updated.",
                outletStatusService.changeStatus(restaurantId, id, request.getStatus())
        ));
    }
}