package com.lavaflow.restaurant.exception;

public class InvalidRestaurantStatusException extends RuntimeException {
    public InvalidRestaurantStatusException(String message) {
        super(message);
    }
}