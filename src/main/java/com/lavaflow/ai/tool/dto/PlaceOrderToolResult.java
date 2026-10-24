package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;

public record PlaceOrderToolResult(
        boolean success,
        String message,
        String orderId,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        String restaurantName,
        String deliveryAddress,
        String paymentMethod
) {
    public static PlaceOrderToolResult success(
            String message,
            String orderId,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            String restaurantName,
            String deliveryAddress,
            String paymentMethod
    ) {
        return new PlaceOrderToolResult(
                true, message, orderId, orderNumber, status,
                totalAmount, restaurantName, deliveryAddress, paymentMethod
        );
    }

    public static PlaceOrderToolResult failure(String message) {
        return new PlaceOrderToolResult(
                false, message, null, null, null, null, null, null, null
        );
    }
}

