package com.lavaflow.kitchen.listener;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.kitchen.service.KitchenOrderService;
import com.lavaflow.order.event.OrderPlacedEvent;
import com.lavaflow.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KitchenOrderListener {

    private final KitchenOrderService kitchenOrderService;

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (event.status() == OrderStatus.KITCHEN_PENDING) {
            kitchenOrderService.createForOrder(event.orderId());
        }
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.to() == OrderStatus.KITCHEN_PENDING) {
            kitchenOrderService.createForOrder(event.orderId());
        } else if (event.to() == OrderStatus.CANCELLED) {
            kitchenOrderService.markCancelledByCustomer(event.orderId());
        }
    }
}
