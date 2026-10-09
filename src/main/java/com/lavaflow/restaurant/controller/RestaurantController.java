package com.lavaflow.restaurant.controller;

import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.restaurant.dto.CreateRestaurantRequest;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.UpdateRestaurantRequest;
import com.lavaflow.restaurant.dto.UpdateRestaurantStatusRequest;
import com.lavaflow.restaurant.service.RestaurantService;
import com.lavaflow.restaurant.service.RestaurantStatusService;
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
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Restaurant management")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantStatusService restaurantStatusService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a restaurant")
    public ResponseEntity<ApiResponse<RestaurantResponse>> create(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created.", restaurantService.createRestaurant(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List restaurants, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<RestaurantResponse>>> getAll(
            @RequestParam(required = false) RestaurantStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<RestaurantResponse> page =
                PageResponse.from(restaurantService.getRestaurants(status, pageable));
        return ResponseEntity.ok(ApiResponse.success("Restaurants fetched.", page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @restaurantAccessPolicy.belongsToRestaurant(authentication, #id)")
    @Operation(summary = "Get a restaurant by id")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant fetched.", restaurantService.getRestaurant(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @restaurantAccessPolicy.isAdminOfRestaurant(authentication, #id)")
    @Operation(summary = "Update restaurant details")
    public ResponseEntity<ApiResponse<RestaurantResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated.", restaurantService.updateRestaurant(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Change restaurant status")
    public ResponseEntity<ApiResponse<RestaurantResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant status updated.",
                restaurantStatusService.changeStatus(id, request.getStatus())
        ));
    }
}