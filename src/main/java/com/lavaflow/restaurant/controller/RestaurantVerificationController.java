package com.lavaflow.restaurant.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.restaurant.dto.RejectRestaurantRequest;
import com.lavaflow.restaurant.dto.RestaurantDocumentDownload;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.RestaurantVerificationResponse;
import com.lavaflow.restaurant.service.RestaurantDocumentService;
import com.lavaflow.restaurant.service.RestaurantVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/verification")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Restaurant Verification", description = "Super admin review of restaurant registrations")
public class RestaurantVerificationController {

    private final RestaurantVerificationService restaurantVerificationService;
    private final RestaurantDocumentService restaurantDocumentService;

    @GetMapping
    @Operation(summary = "Get restaurant details, admin contact and submitted documents")
    public ResponseEntity<ApiResponse<RestaurantVerificationResponse>> getVerification(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Verification details fetched.",
                restaurantVerificationService.getVerification(restaurantId)
        ));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "View a submitted document")
    public ResponseEntity<Resource> getDocument(
            @PathVariable UUID restaurantId,
            @PathVariable UUID documentId
    ) {
        RestaurantDocumentDownload download = restaurantDocumentService.getDownload(restaurantId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve a restaurant registration")
    public ResponseEntity<ApiResponse<RestaurantResponse>> approve(
            @PathVariable UUID restaurantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant approved.",
                restaurantVerificationService.approve(restaurantId, principal.getUser().getId())
        ));
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject a restaurant registration with a reason")
    public ResponseEntity<ApiResponse<RestaurantResponse>> reject(
            @PathVariable UUID restaurantId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RejectRestaurantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant rejected.",
                restaurantVerificationService.reject(restaurantId, principal.getUser().getId(), request.getReason())
        ));
    }
}