package com.lavaflow.inventory.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.common.response.PageResponse;
import com.lavaflow.inventory.dto.AdjustInventoryRequest;
import com.lavaflow.inventory.dto.CreateInventoryRequest;
import com.lavaflow.inventory.dto.InventoryResponse;
import com.lavaflow.inventory.dto.UpdateInventoryRequest;
import com.lavaflow.inventory.service.InventoryService;
import com.lavaflow.inventory.service.InventoryStockService;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management per outlet for the authenticated restaurant")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryStockService inventoryStockService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Start tracking stock of a menu item at an outlet")
    public ResponseEntity<ApiResponse<InventoryResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Stock record created.",
                inventoryService.createInventory(restaurantId, request)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "List stock records, filtered by outlet, menu item or low stock")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) UUID menuItemId,
            @RequestParam(required = false) Boolean lowStock,
            @PageableDefault(size = 20, sort = "menuItem.name") Pageable pageable
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        PageResponse<InventoryResponse> page = PageResponse.from(
                inventoryService.getInventory(restaurantId, outletId, menuItemId, lowStock, pageable)
        );
        return ResponseEntity.ok(ApiResponse.success("Stock records fetched.", page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Get a stock record by id")
    public ResponseEntity<ApiResponse<InventoryResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Stock record fetched.",
                inventoryService.getInventoryItem(restaurantId, id)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Set the stock quantity and low stock threshold")
    public ResponseEntity<ApiResponse<InventoryResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Stock updated.",
                inventoryStockService.updateStock(restaurantId, id, request)
        ));
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Add or remove units from the stock quantity")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjust(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Stock adjusted.",
                inventoryStockService.adjustStock(restaurantId, id, request.getDelta())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Stop tracking stock for a menu item at an outlet")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        inventoryService.deleteInventory(restaurantId, id);
        return ResponseEntity.ok(ApiResponse.success("Stock record removed."));
    }
}