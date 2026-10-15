package com.lavaflow.order.service;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.OrderSummaryResponse;
import com.lavaflow.order.mapper.OrderMapper;
import com.lavaflow.order.repository.OrderRepository;
import com.lavaflow.order.repository.OrderSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public Page<OrderSummaryResponse> getCustomerOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        return orderRepository
                .findAll(OrderSpecifications.forCustomer(customerId, status), pageable)
                .map(orderMapper::toSummary);
    }

    public OrderResponse getCustomerOrder(UUID customerId, UUID orderId) {
        return orderRepository.findByIdAndCustomerId(orderId, customerId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));
    }

    public Page<OrderSummaryResponse> getRestaurantOrders(
            UUID restaurantId,
            UUID outletId,
            OrderStatus status,
            Pageable pageable
    ) {
        return orderRepository
                .findAll(OrderSpecifications.forRestaurant(restaurantId, outletId, status), pageable)
                .map(orderMapper::toSummary);
    }

    public OrderResponse getRestaurantOrder(UUID restaurantId, UUID orderId) {
        return orderRepository.findByIdAndRestaurantId(orderId, restaurantId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order not found."));
    }
}
