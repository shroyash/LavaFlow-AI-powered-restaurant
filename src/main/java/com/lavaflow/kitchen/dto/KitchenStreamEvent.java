package com.lavaflow.kitchen.dto;

import java.util.UUID;

public record KitchenStreamEvent(KitchenEventType type, UUID kitchenOrderId, UUID orderId) {
}
