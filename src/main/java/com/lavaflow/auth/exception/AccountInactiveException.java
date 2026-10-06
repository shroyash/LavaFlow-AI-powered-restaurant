package com.lavaflow.auth.exception;

/**
 * Thrown when a user's account has is_active = false.
 */
public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
