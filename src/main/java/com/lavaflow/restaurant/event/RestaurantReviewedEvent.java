package com.lavaflow.restaurant.event;

public record RestaurantReviewedEvent(
        String adminEmail,
        String adminName,
        String restaurantName,
        boolean approved,
        String rejectionReason
) {
}