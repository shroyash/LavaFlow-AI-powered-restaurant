package com.lavaflow.restaurant.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.restaurant.dto.CreateKitchenStaffRequest;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import com.lavaflow.restaurant.service.KitchenStaffService;
import com.lavaflow.restaurant.service.RestaurantStaffQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/me/staff")
@PreAuthorize("hasRole('RESTAURANT_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Restaurant Staff", description = "Staff management for the authenticated restaurant admin")
public class RestaurantStaffController {

    private final KitchenStaffService kitchenStaffService;
    private final RestaurantStaffQueryService restaurantStaffQueryService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    /**
     * GET /api/v1/restaurants/me/staff
     * Returns all staff accounts (KITCHEN_STAFF, RESTAURANT_STAFF) belonging to the admin's restaurant.
     */
    @GetMapping
    @Operation(summary = "List all staff belonging to the authenticated restaurant")
    public ResponseEntity<ApiResponse<List<KitchenStaffResponse>>> listStaff(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        List<KitchenStaffResponse> staff = restaurantStaffQueryService.getRestaurantStaff(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Staff fetched.", staff));
    }

    /**
     * GET /api/v1/restaurants/me/staff/{staffId}
     * Returns a single staff member by ID, scoped to the restaurant.
     */
    @GetMapping("/{staffId}")
    @Operation(summary = "Get a staff member by ID")
    public ResponseEntity<ApiResponse<KitchenStaffResponse>> getStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID staffId
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        KitchenStaffResponse staff = restaurantStaffQueryService.getStaffMember(restaurantId, staffId);
        return ResponseEntity.ok(ApiResponse.success("Staff member fetched.", staff));
    }

    /**
     * POST /api/v1/restaurants/me/staff
     * Creates a new KITCHEN_STAFF account for the authenticated restaurant.
     * Delegates to the existing KitchenStaffService.
     */
    @PostMapping
    @Operation(summary = "Create a kitchen staff account for the admin's restaurant")
    public ResponseEntity<ApiResponse<KitchenStaffResponse>> createStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateKitchenStaffRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        KitchenStaffResponse response = kitchenStaffService.createKitchenStaff(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kitchen staff created.", response));
    }
}

