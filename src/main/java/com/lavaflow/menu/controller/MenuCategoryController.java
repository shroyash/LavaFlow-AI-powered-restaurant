package com.lavaflow.menu.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.menu.dto.CreateMenuCategoryRequest;
import com.lavaflow.menu.dto.MenuCategoryResponse;
import com.lavaflow.menu.dto.UpdateMenuCategoryRequest;
import com.lavaflow.menu.service.MenuCategoryService;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu/categories")
@RequiredArgsConstructor
@Tag(name = "Menu Categories", description = "Menu category management for the authenticated restaurant")
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Create a menu category")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMenuCategoryRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Category created.",
                menuCategoryService.createCategory(restaurantId, request)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "List categories in display order, optionally filtered by active flag")
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean active
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Categories fetched.",
                menuCategoryService.getCategories(restaurantId, active)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Get a category by id")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Category fetched.",
                menuCategoryService.getCategory(restaurantId, id)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuCategoryRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Category updated.",
                menuCategoryService.updateCategory(restaurantId, id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Delete a category that has no menu items")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        menuCategoryService.deleteCategory(restaurantId, id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted."));
    }
}