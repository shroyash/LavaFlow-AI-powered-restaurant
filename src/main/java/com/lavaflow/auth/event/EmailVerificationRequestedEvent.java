package com.lavaflow.auth.event;

public record EmailVerificationRequestedEvent(String email, String fullName, String token) {
}