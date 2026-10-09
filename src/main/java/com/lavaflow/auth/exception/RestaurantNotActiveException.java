package com.lavaflow.auth.exception;

public class RestaurantNotActiveException extends AccountInactiveException {
    public RestaurantNotActiveException(String message) {
        super(message);
    }
}