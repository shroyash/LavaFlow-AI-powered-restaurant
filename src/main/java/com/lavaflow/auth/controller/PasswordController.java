package com.lavaflow.auth.controller;

import com.lavaflow.auth.dto.ChangePasswordRequest;
import com.lavaflow.auth.dto.ForgotPasswordRequest;
import com.lavaflow.auth.dto.ResetPasswordRequest;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.auth.service.PasswordChangeService;
import com.lavaflow.auth.service.PasswordResetService;
import com.lavaflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Password", description = "Change, forgot and reset password")
public class PasswordController {

    private final PasswordChangeService passwordChangeService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/change-password")
    @Operation(summary = "Change the password of the logged-in user; all sessions are signed out")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        passwordChangeService.changePassword(
                principal.getUser().getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Password changed. Please log in again."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this email, a password reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using the token from the reset email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now log in."));
    }
}