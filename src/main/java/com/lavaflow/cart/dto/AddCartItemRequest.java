package com.lavaflow.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull(message = "Menu item ID is required.")
        UUID menuItemId,

        @Min(value = 1, message = "Quantity must be at least 1.")
        @Max(value = 100, message = "Quantity cannot exceed 100.")
        int quantity
) {
}