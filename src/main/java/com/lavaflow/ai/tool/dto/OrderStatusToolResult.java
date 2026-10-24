package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderStatusToolResult(
        boolean success,
        String message,
        String orderId,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        String restaurantName,
        String outletName,
        String deliveryAddress,
        LocalDateTime createdAt
) {
    public static OrderStatusToolResult success(
            String message,
            String orderId,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            String restaurantName,
            String outletName,
            String deliveryAddress,
            LocalDateTime createdAt
    ) {
        return new OrderStatusToolResult(
                true, message, orderId, orderNumber, status,
                totalAmount, restaurantName, outletName, deliveryAddress, createdAt
        );
    }

    public static OrderStatusToolResult failure(String message) {
        return new OrderStatusToolResult(
                false, message, null, null, null, null, null, null, null, null
        );
    }
}

