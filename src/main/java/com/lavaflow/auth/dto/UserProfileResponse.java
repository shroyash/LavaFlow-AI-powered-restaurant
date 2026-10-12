package com.lavaflow.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {

    private String userId;
    private String email;
    private boolean emailVerified;
    private String fullName;
    private String phone;
    private String role;
    private String restaurantId;
    private String restaurantName;
    private LocalDateTime createdAt;
}