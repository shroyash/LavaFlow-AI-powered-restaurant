package com.lavaflow.kitchen.listener;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.kitchen.dto.KitchenEventType;
import com.lavaflow.kitchen.dto.KitchenStreamEvent;
import com.lavaflow.kitchen.repository.KitchenOrderRepository;
import com.lavaflow.kitchen.service.KitchenStreamRegistry;
import com.lavaflow.order.event.OrderPlacedEvent;
import com.lavaflow.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KitchenStreamNotifier {

    private final KitchenStreamRegistry kitchenStreamRegistry;
    private final KitchenOrderRepository kitchenOrderRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (event.status() == OrderStatus.KITCHEN_PENDING) {
            publish(event.restaurantId(), event.orderId(), KitchenEventType.NEW_ORDER);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        publish(event.restaurantId(), event.orderId(), typeFor(event.to()));
    }

    private KitchenEventType typeFor(OrderStatus status) {
        return switch (status) {
            case KITCHEN_PENDING -> KitchenEventType.NEW_ORDER;
            case CANCELLED, KITCHEN_REJECTED -> KitchenEventType.ORDER_CANCELLED;
            default -> KitchenEventType.ORDER_UPDATED;
        };
    }

    private void publish(UUID restaurantId, UUID orderId, KitchenEventType type) {
        kitchenOrderRepository.findByOrderId(orderId).ifPresent(kitchenOrder ->
                kitchenStreamRegistry.broadcast(
                        restaurantId,
                        new KitchenStreamEvent(type, kitchenOrder.getId(), orderId)
                )
        );
    }
}
