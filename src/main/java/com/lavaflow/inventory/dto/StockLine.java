package com.lavaflow.inventory.dto;

import java.util.UUID;

public record StockLine(UUID menuItemId, int quantity) {

    public StockLine {
        if (menuItemId == null) {
            throw new IllegalArgumentException("Menu item is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }
}