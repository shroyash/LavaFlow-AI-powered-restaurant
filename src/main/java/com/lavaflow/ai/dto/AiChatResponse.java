
package com.lavaflow.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        UUID conversationId,
        String reply,
        List<RestaurantOption> restaurants
) {
    public record RestaurantOption(
            UUID id,
            String name,
            String description,
            UUID outletId,
            String outletName,
            String outletAddress,
            String distanceKm,
            Integer estimatedDeliveryMinutes
    ) {
    }
}
