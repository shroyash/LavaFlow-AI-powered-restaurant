package com.lavaflow.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication response returned on successful login or token refresh.
 * Does NOT include password hash or sensitive user data.
 */
@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;  // seconds until access token expires
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private String userId;
        private String email;
        private String fullName;
        private String role;
        private String restaurantId; // null for SUPER_ADMIN and unscoped CUSTOMER
    }
}
