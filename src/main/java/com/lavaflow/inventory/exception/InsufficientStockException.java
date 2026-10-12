package com.lavaflow.inventory.exception;

import com.lavaflow.common.exception.ConflictException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class InsufficientStockException extends ConflictException {

    private final UUID menuItemId;
    private final int availableQuantity;

    public InsufficientStockException(UUID menuItemId, int availableQuantity) {
        super(availableQuantity > 0
                ? "Only " + availableQuantity + " left in stock."
                : "This item is out of stock at this outlet.");
        this.menuItemId = menuItemId;
        this.availableQuantity = availableQuantity;
    }
}