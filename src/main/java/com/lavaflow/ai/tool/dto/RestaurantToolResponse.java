package com.lavaflow.ai.tool.dto;

import java.util.UUID;

public record RestaurantToolResponse(
        UUID id,
        String name,
        String description,
        String status
) {
}

