package com.lavaflow.restaurant.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.restaurant.dto.CreateKitchenStaffRequest;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import com.lavaflow.restaurant.service.KitchenStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/me/kitchen-staff")
@RequiredArgsConstructor
@Tag(name = "Kitchen Staff", description = "Kitchen staff management for restaurant admins")
public class KitchenStaffController {

    private final KitchenStaffService kitchenStaffService;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Create a kitchen staff account for the admin's restaurant")
    public ResponseEntity<ApiResponse<KitchenStaffResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateKitchenStaffRequest request
    ) {
        UUID restaurantId = principal.getUser().getRestaurant().getId();
        KitchenStaffResponse response = kitchenStaffService.createKitchenStaff(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kitchen staff created.", response));
    }
}