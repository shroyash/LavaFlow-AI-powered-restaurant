package com.lavaflow.order.service;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.entity.Order;
import com.lavaflow.order.exception.InvalidOrderStatusException;
import com.lavaflow.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            Set.of(OrderStatus.CREATED, OrderStatus.PENDING_KITCHEN);

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;

    @Transactional
    public OrderResponse cancelByCustomer(UUID customerId, UUID orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStatusException(
                    "This order can no longer be cancelled because the restaurant has already started on it."
            );
        }

        return orderStatusService.transition(orderId, OrderStatus.CANCELLED);
    }
}
