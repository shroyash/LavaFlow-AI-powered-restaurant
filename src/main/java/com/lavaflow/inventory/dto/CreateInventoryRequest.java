package com.lavaflow.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateInventoryRequest {

    @NotNull(message = "Outlet is required")
    private UUID outletId;

    @NotNull(message = "Menu item is required")
    private UUID menuItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 1000000, message = "Quantity is too large")
    private Integer quantity;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Max(value = 1000000, message = "Low stock threshold is too large")
    private Integer lowStockThreshold;
}