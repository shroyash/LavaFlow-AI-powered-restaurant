package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemToolResponse(
        UUID menuItemId,
        String name,
        String categoryName,
        String description,
        BigDecimal price,
        boolean available
) {
}