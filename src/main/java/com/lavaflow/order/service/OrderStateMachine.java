package com.lavaflow.order.service;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.exception.InvalidOrderStatusException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.lavaflow.common.enums.OrderStatus.ACCEPTED;
import static com.lavaflow.common.enums.OrderStatus.CANCELLED;
import static com.lavaflow.common.enums.OrderStatus.CREATED;
import static com.lavaflow.common.enums.OrderStatus.DELIVERED;
import static com.lavaflow.common.enums.OrderStatus.OUT_FOR_DELIVERY;
import static com.lavaflow.common.enums.OrderStatus.PENDING_KITCHEN;
import static com.lavaflow.common.enums.OrderStatus.PICKED_UP;
import static com.lavaflow.common.enums.OrderStatus.PREPARING;
import static com.lavaflow.common.enums.OrderStatus.READY;
import static com.lavaflow.common.enums.OrderStatus.REJECTED;
import static com.lavaflow.common.enums.OrderStatus.RIDER_ASSIGNED;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            CREATED, EnumSet.of(PENDING_KITCHEN, CANCELLED),
            PENDING_KITCHEN, EnumSet.of(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED, EnumSet.of(PREPARING),
            PREPARING, EnumSet.of(READY),
            READY, EnumSet.of(RIDER_ASSIGNED),
            RIDER_ASSIGNED, EnumSet.of(PICKED_UP),
            PICKED_UP, EnumSet.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED)
    );

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void requireTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidOrderStatusException(from, to);
        }
    }
}
