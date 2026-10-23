package com.lavaflow.ai.tool.dto;

import com.lavaflow.cart.dto.CartResponse;

public record CartToolResult(
        boolean success,
        String message,
        CartResponse cart
) {
    public static CartToolResult success(String message, CartResponse cart) {
        return new CartToolResult(true, message, cart);
    }

    public static CartToolResult failure(String message) {
        return new CartToolResult(false, message, null);
    }
}