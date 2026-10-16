
package com.lavaflow.order.service;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.service.InventoryReservationService;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.entity.Order;
import com.lavaflow.order.event.OrderStatusChangedEvent;
import com.lavaflow.order.mapper.OrderMapper;
import com.lavaflow.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.lavaflow.common.enums.OrderStatus.*;

@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private static final Set<OrderStatus> STOCK_CONSUMED_STATUSES =
            Set.of(
                    KITCHEN_ACCEPTED,
                    PREPARING,
                    READY_FOR_PICKUP,
                    OUT_FOR_DELIVERY,
                    DELIVERED
            );

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final InventoryReservationService inventoryReservationService;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse transition(UUID orderId, OrderStatus target) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Order not found.")
                );

        OrderStatus from = order.getStatus();

        // 1. Validate the requested transition.
        orderStateMachine.requireTransition(from, target);

        // 2. Apply inventory changes in the same transaction.
        applyInventoryEffects(order, from, target);

        // 3. Update the order status.
        order.setStatus(target);

        // The entity is managed, so save() is optional here.
        // Keeping it is also valid.
        Order saved = orderRepository.save(order);

        // 4. Notify other application components.
        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(
                        saved.getId(),
                        saved.getRestaurant().getId(),
                        from,
                        target
                )
        );

        return orderMapper.toResponse(saved);
    }

    private void applyInventoryEffects(
            Order order,
            OrderStatus from,
            OrderStatus target
    ) {
        UUID outletId = order.getOutlet().getId();
        List<StockLine> lines = stockLines(order);

        // Consume reserved stock when the kitchen accepts the order.
        if (target == KITCHEN_ACCEPTED) {
            inventoryReservationService.consumeAll(outletId, lines);
            return;
        }

        // Release reservations if the order is cancelled or rejected
        // before the reservation has been consumed.
        if ((target == CANCELLED || target == KITCHEN_REJECTED)
                && !STOCK_CONSUMED_STATUSES.contains(from)) {
            inventoryReservationService.releaseAll(outletId, lines);
        }
    }

    private List<StockLine> stockLines(Order order) {
        return order.getItems().stream()
                .filter(item -> item.getMenuItem() != null)
                .map(item -> new StockLine(
                        item.getMenuItem().getId(),
                        item.getQuantity()
                ))
                .toList();
    }
}
