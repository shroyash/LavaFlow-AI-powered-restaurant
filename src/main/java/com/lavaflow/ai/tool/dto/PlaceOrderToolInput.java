package com.lavaflow.ai.tool.dto;

import java.util.List;
import java.util.UUID;

public record PlaceOrderToolInput(
        UUID restaurantId,
        UUID addressId,
        String paymentMethod,
        String specialInstructions,
        String idempotencyKey,
        List<OrderItemInput> items
) {
    public PlaceOrderToolInput() {
        this(null, null, null, null, null, null);
    }

    public record OrderItemInput(
            UUID menuItemId,
            Integer quantity
    ) {}
}


