package com.lavaflow.restaurant.event;

import java.util.UUID;

public record RestaurantAccessRevokedEvent(UUID restaurantId) {
}