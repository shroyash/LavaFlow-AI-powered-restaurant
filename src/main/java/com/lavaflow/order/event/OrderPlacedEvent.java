package com.lavaflow.order.event;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID restaurantId,
        UUID outletId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount
) {
}
