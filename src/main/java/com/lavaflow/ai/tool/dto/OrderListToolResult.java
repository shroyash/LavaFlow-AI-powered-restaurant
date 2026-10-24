package com.lavaflow.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderListToolResult(
        boolean success,
        String message,
        List<OrderSummaryItem> orders
) {
    public record OrderSummaryItem(
            String id,
            String orderNumber,
            String status,
            String restaurantName,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime createdAt
    ) {}

    public static OrderListToolResult success(List<OrderSummaryItem> orders) {
        return new OrderListToolResult(true, "Orders retrieved successfully.", orders);
    }

    public static OrderListToolResult empty() {
        return new OrderListToolResult(true, "No past orders found.", List.of());
    }

    public static OrderListToolResult failure(String message) {
        return new OrderListToolResult(false, message, List.of());
    }
}

