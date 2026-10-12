package com.lavaflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 200)
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String newPassword;
}