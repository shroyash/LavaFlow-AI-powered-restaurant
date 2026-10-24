package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSummaryToolResult(
        boolean success,
        String message,
        UUID restaurantId,
        String restaurantName,
        List<CartItemSummary> items,
        BigDecimal subtotal,
        int totalItems,
        String deliveryAddress,
        UUID deliveryAddressId,
        String paymentMethod,
        boolean readyToPlace
) {
    public record CartItemSummary(
            UUID menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public static OrderSummaryToolResult success(
            String message,
            UUID restaurantId,
            String restaurantName,
            List<CartItemSummary> items,
            BigDecimal subtotal,
            int totalItems,
            String deliveryAddress,
            UUID deliveryAddressId,
            String paymentMethod,
            boolean readyToPlace
    ) {
        return new OrderSummaryToolResult(
                true, message, restaurantId, restaurantName, items,
                subtotal, totalItems, deliveryAddress, deliveryAddressId,
                paymentMethod, readyToPlace
        );
    }

    public static OrderSummaryToolResult failure(String message) {
        return new OrderSummaryToolResult(
                false, message, null, null, List.of(),
                BigDecimal.ZERO, 0, null, null, null, false
        );
    }
}

