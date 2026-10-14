package com.lavaflow.delivery.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.delivery.dto.NearbyRestaurantRequest;
import com.lavaflow.delivery.dto.NearbyRestaurantResponse;
import com.lavaflow.delivery.service.NearbyRestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery coverage and outlet matching")
public class NearbyRestaurantController {

    private final NearbyRestaurantService nearbyRestaurantService;

    @PostMapping("/nearby-restaurants")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Find restaurants that deliver to a location, closest first, optionally filtered by name or dish")
    public ResponseEntity<ApiResponse<List<NearbyRestaurantResponse>>> findNearbyRestaurants(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NearbyRestaurantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Nearby restaurants fetched.",
                nearbyRestaurantService.locate(principal.getUser().getId(), request)
        ));
    }
}