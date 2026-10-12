package com.lavaflow.auth.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.user.dto.UpdateProfileRequest;
import com.lavaflow.user.dto.UserProfileResponse;
import com.lavaflow.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "My Profile", description = "Profile of the logged-in user (customer, staff, kitchen, rider, admin)")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "Get my profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile fetched.",
                userProfileService.getProfile(principal.getUser().getId())
        ));
    }

    @PutMapping
    @Operation(summary = "Update my name and phone")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated.",
                userProfileService.updateProfile(principal.getUser().getId(), request)
        ));
    }
}