package com.lavaflow.menu.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.menu.dto.CreateMenuItemRequest;
import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.dto.UpdateMenuItemAvailabilityRequest;
import com.lavaflow.menu.dto.UpdateMenuItemRequest;
import com.lavaflow.menu.service.MenuItemAvailabilityService;
import com.lavaflow.menu.service.MenuItemService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/menu/items")
@RequiredArgsConstructor
@Tag(name = "Menu Items", description = "Menu item management for the authenticated restaurant")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemAvailabilityService menuItemAvailabilityService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Create a menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Menu item created.",
                menuItemService.createItem(restaurantId, request)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "List menu items, filtered by category, availability or name")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        PageResponse<MenuItemResponse> page = PageResponse.from(
                menuItemService.getItems(restaurantId, categoryId, available, query, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success("Menu items fetched.", page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Get a menu item by id")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Menu item fetched.",
                menuItemService.getItem(restaurantId, id)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Update a menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Menu item updated.",
                menuItemService.updateItem(restaurantId, id, request)
        ));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Mark a menu item available or unavailable")
    public ResponseEntity<ApiResponse<MenuItemResponse>> changeAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemAvailabilityRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Menu item availability updated.",
                menuItemAvailabilityService.changeAvailability(restaurantId, id, request.getAvailable())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Delete a menu item")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        menuItemService.deleteItem(restaurantId, id);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted."));
    }
}