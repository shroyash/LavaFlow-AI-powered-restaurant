package com.lavaflow.ai.tool.dto;

import java.util.UUID;

public record CartToolInput(
        String action,
        UUID menuItemId,
        String itemName,
        Integer quantity
) {
    public CartToolInput(String action, UUID menuItemId, Integer quantity) {
        this(action, menuItemId, null, quantity);
    }
}
