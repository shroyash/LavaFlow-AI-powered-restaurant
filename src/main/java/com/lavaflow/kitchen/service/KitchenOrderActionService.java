package com.lavaflow.kitchen.service;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.kitchen.dto.KitchenOrderResponse;
import com.lavaflow.kitchen.entity.KitchenOrder;
import com.lavaflow.kitchen.mapper.KitchenOrderMapper;
import com.lavaflow.kitchen.repository.KitchenOrderRepository;
import com.lavaflow.order.service.OrderStatusService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenOrderActionService {

    private static final String TIMEOUT_REASON = "The restaurant did not respond in time.";

    private final KitchenOrderRepository kitchenOrderRepository;
    private final KitchenStateMachine kitchenStateMachine;
    private final OrderStatusService orderStatusService;
    private final KitchenOrderMapper kitchenOrderMapper;

    @Transactional
    public KitchenOrderResponse accept(UUID restaurantId, UUID kitchenOrderId) {
        return advance(findOwned(restaurantId, kitchenOrderId),
                KitchenOrderStatus.ACCEPTED, OrderStatus.KITCHEN_ACCEPTED, null);
    }

    @Transactional
    public KitchenOrderResponse reject(UUID restaurantId, UUID kitchenOrderId, String reason) {
        return advance(findOwned(restaurantId, kitchenOrderId),
                KitchenOrderStatus.REJECTED, OrderStatus.KITCHEN_REJECTED, reason.trim());
    }

    @Transactional
    public KitchenOrderResponse startPreparing(UUID restaurantId, UUID kitchenOrderId) {
        return advance(findOwned(restaurantId, kitchenOrderId),
                KitchenOrderStatus.PREPARING, OrderStatus.PREPARING, null);
    }

    @Transactional
    public KitchenOrderResponse markReady(UUID restaurantId, UUID kitchenOrderId) {
        return advance(findOwned(restaurantId, kitchenOrderId),
                KitchenOrderStatus.READY, OrderStatus.READY_FOR_PICKUP, null);
    }

    @Transactional
    public void autoReject(UUID kitchenOrderId) {
        KitchenOrder kitchenOrder = kitchenOrderRepository.findById(kitchenOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Kitchen order not found."));

        advance(kitchenOrder, KitchenOrderStatus.REJECTED, OrderStatus.KITCHEN_REJECTED, TIMEOUT_REASON);
    }

    private KitchenOrderResponse advance(
            KitchenOrder kitchenOrder,
            KitchenOrderStatus target,
            OrderStatus orderTarget,
            String reason
    ) {
        kitchenStateMachine.requireTransition(kitchenOrder.getStatus(), target);

        orderStatusService.transition(kitchenOrder.getOrder().getId(), orderTarget);

        LocalDateTime now = LocalDateTime.now();
        kitchenOrder.setStatus(target);

        switch (target) {
            case ACCEPTED -> kitchenOrder.setAcceptedAt(now);
            case PREPARING -> kitchenOrder.setPreparingAt(now);
            case READY -> kitchenOrder.setReadyAt(now);
            case REJECTED -> {
                kitchenOrder.setRejectedAt(now);
                kitchenOrder.setRejectionReason(reason);
            }
            default -> {
            }
        }

        return kitchenOrderMapper.toResponse(kitchenOrderRepository.save(kitchenOrder));
    }

    private KitchenOrder findOwned(UUID restaurantId, UUID kitchenOrderId) {
        return kitchenOrderRepository.findByIdAndRestaurantId(kitchenOrderId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Kitchen order not found."));
    }
}
