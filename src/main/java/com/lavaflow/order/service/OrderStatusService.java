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

@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private static final Set<OrderStatus> STOCK_RELEASING_STATUSES = Set.of(OrderStatus.CANCELLED, OrderStatus.REJECTED);

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final InventoryReservationService inventoryReservationService;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse transition(UUID orderId, OrderStatus target) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));

        OrderStatus from = order.getStatus();
        orderStateMachine.requireTransition(from, target);

        applyInventoryEffects(order, target);

        order.setStatus(target);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(saved.getId(), saved.getRestaurant().getId(), from, target)
        );

        return orderMapper.toResponse(saved);
    }

    private void applyInventoryEffects(Order order, OrderStatus target) {
        UUID outletId = order.getOutlet().getId();

        if (target == OrderStatus.ACCEPTED) {
            inventoryReservationService.consumeAll(outletId, stockLines(order));
        } else if (STOCK_RELEASING_STATUSES.contains(target)) {
            inventoryReservationService.releaseAll(outletId, stockLines(order));
        }
    }

    private List<StockLine> stockLines(Order order) {
        return order.getItems().stream()
                .filter(item -> item.getMenuItem() != null)
                .map(item -> new StockLine(item.getMenuItem().getId(), item.getQuantity()))
                .toList();
    }
}
