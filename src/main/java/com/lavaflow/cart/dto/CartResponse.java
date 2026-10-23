package com.lavaflow.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID restaurantId,
        String restaurantName,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        int totalItems
) {

    public record CartItemResponse(
            UUID cartItemId,
            UUID menuItemId,
            String name,
            String description,
            String imageUrl,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            boolean available
    ) {
    }
}