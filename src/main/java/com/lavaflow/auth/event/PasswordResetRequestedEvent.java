package com.lavaflow.auth.event;

public record PasswordResetRequestedEvent(String email, String fullName, String token) {
}