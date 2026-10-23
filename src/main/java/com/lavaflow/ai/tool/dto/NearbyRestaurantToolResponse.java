package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NearbyRestaurantToolResponse(
        UUID restaurantId,
        String restaurantName,
        String description,
        UUID outletId,
        String outletName,
        String outletAddress,
        BigDecimal distanceKm,
        int estimatedDeliveryMinutes,
        boolean busy
) {
}

