package com.lavaflow.order.event;

import com.lavaflow.common.enums.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        UUID restaurantId,
        OrderStatus from,
        OrderStatus to
) {
}
