package com.lavaflow.restaurant.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.UpdateRestaurantRequest;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import com.lavaflow.restaurant.service.RestaurantLogoService;
import com.lavaflow.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/me")
@RequiredArgsConstructor
@Tag(name = "Restaurant Profile", description = "Profile of the authenticated user's restaurant")
public class RestaurantProfileController {

    private final RestaurantService restaurantService;
    private final RestaurantLogoService restaurantLogoService;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'RESTAURANT_STAFF', 'KITCHEN_STAFF')")
    @Operation(summary = "Get my restaurant profile")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant profile fetched.",
                restaurantService.getRestaurant(restaurantId)
        ));
    }

    @PutMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Update my restaurant's name, description, phone and email")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateRestaurantRequest request
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant profile updated.",
                restaurantService.updateRestaurant(restaurantId, request)
        ));
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Upload or replace the restaurant logo (JPG or PNG)")
    public ResponseEntity<ApiResponse<RestaurantResponse>> uploadLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant logo updated.",
                restaurantLogoService.uploadLogo(restaurantId, file)
        ));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    @Operation(summary = "Remove the restaurant logo")
    public ResponseEntity<ApiResponse<RestaurantResponse>> removeLogo(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant logo removed.",
                restaurantLogoService.removeLogo(restaurantId)
        ));
    }
}