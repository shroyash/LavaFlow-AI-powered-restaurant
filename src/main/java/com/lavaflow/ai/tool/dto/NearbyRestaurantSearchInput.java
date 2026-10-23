package com.lavaflow.ai.tool.dto;

import java.util.UUID;

public record NearbyRestaurantSearchInput(
        UUID addressId,
        String manualAddressLine,
        String query
) {
}

