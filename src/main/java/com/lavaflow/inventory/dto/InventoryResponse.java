package com.lavaflow.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryResponse {

    private String id;
    private String restaurantId;
    private String outletId;
    private String outletName;
    private String menuItemId;
    private String menuItemName;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int lowStockThreshold;
    private boolean lowStock;
    private LocalDateTime updatedAt;
}