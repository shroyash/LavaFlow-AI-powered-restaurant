package com.lavaflow.kitchen.service;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.kitchen.dto.KitchenOrderResponse;
import com.lavaflow.kitchen.entity.KitchenOrder;
import com.lavaflow.kitchen.mapper.KitchenOrderMapper;
import com.lavaflow.kitchen.repository.KitchenOrderRepository;
import com.lavaflow.kitchen.repository.KitchenOrderSpecifications;
import com.lavaflow.order.entity.Order;
import com.lavaflow.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenOrderService {

    private static final String CUSTOMER_CANCELLED_REASON = "Cancelled by the customer.";

    private final KitchenOrderRepository kitchenOrderRepository;
    private final OrderRepository orderRepository;
    private final KitchenOrderMapper kitchenOrderMapper;

    @Transactional
    public void createForOrder(UUID orderId) {
        if (kitchenOrderRepository.existsByOrderId(orderId)) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));

        KitchenOrder kitchenOrder = new KitchenOrder();
        kitchenOrder.setOrder(order);
        kitchenOrder.setRestaurant(order.getRestaurant());
        kitchenOrder.setOutlet(order.getOutlet());

        kitchenOrderRepository.save(kitchenOrder);
    }

    @Transactional
    public void markCancelledByCustomer(UUID orderId) {
        kitchenOrderRepository.findByOrderId(orderId)
                .filter(kitchenOrder -> kitchenOrder.getStatus() == KitchenOrderStatus.PENDING)
                .ifPresent(kitchenOrder -> {
                    kitchenOrder.setStatus(KitchenOrderStatus.REJECTED);
                    kitchenOrder.setRejectedAt(LocalDateTime.now());
                    kitchenOrder.setRejectionReason(CUSTOMER_CANCELLED_REASON);
                    kitchenOrderRepository.save(kitchenOrder);
                });
    }

    @Transactional(readOnly = true)
    public Page<KitchenOrderResponse> getKitchenOrders(
            UUID restaurantId,
            UUID outletId,
            KitchenOrderStatus status,
            boolean activeOnly,
            Pageable pageable
    ) {
        return kitchenOrderRepository
                .findAll(KitchenOrderSpecifications.filter(restaurantId, outletId, status, activeOnly), pageable)
                .map(kitchenOrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public KitchenOrderResponse getKitchenOrder(UUID restaurantId, UUID kitchenOrderId) {
        return kitchenOrderRepository.findByIdAndRestaurantId(kitchenOrderId, restaurantId)
                .map(kitchenOrderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Kitchen order not found."));
    }
}
