package com.lavaflow.ai.tool.dto;

import java.util.List;

public record NearbyRestaurantListResult(
        boolean success,
        String message,
        List<NearbyRestaurantToolResponse> restaurants
) {
    public static NearbyRestaurantListResult success(List<NearbyRestaurantToolResponse> restaurants) {
        return new NearbyRestaurantListResult(true, "Nearby restaurants retrieved successfully.", restaurants);
    }

    public static NearbyRestaurantListResult error(String message) {
        return new NearbyRestaurantListResult(false, message, List.of());
    }

    public static NearbyRestaurantListResult validationError(String message) {
        return new NearbyRestaurantListResult(false, message, List.of());
    }

    public static NearbyRestaurantListResult noAddressProvided(String message) {
        return new NearbyRestaurantListResult(false, message, List.of());
    }
}

