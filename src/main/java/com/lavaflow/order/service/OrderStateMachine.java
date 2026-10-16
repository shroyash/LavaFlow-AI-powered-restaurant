
package com.lavaflow.order.service;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.exception.InvalidOrderStatusException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.lavaflow.common.enums.OrderStatus.*;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS =
            Map.ofEntries(
                    Map.entry(CREATED, EnumSet.of(
                            PAYMENT_PENDING, PLACED, CANCELLED
                    )),
                    Map.entry(PAYMENT_PENDING, EnumSet.of(
                            PAYMENT_CONFIRMED, CANCELLED
                    )),
                    Map.entry(PAYMENT_CONFIRMED, EnumSet.of(
                            PLACED, CANCELLED
                    )),
                    Map.entry(PLACED, EnumSet.of(
                            ALLOCATED, KITCHEN_PENDING, CANCELLED
                    )),
                    Map.entry(ALLOCATED, EnumSet.of(
                            KITCHEN_PENDING, CANCELLED
                    )),
                    Map.entry(KITCHEN_PENDING, EnumSet.of(
                            KITCHEN_ACCEPTED, KITCHEN_REJECTED, CANCELLED
                    )),
                    Map.entry(KITCHEN_ACCEPTED, EnumSet.of(
                            PREPARING, CANCELLED
                    )),
                    Map.entry(PREPARING, EnumSet.of(
                            READY_FOR_PICKUP, CANCELLED
                    )),
                    Map.entry(READY_FOR_PICKUP, EnumSet.of(
                            OUT_FOR_DELIVERY, CANCELLED
                    )),
                    Map.entry(OUT_FOR_DELIVERY, EnumSet.of(
                            DELIVERED
                    )),
                    Map.entry(KITCHEN_REJECTED, EnumSet.of(
                            CANCELLED
                    )),
                    Map.entry(CANCELLED, EnumSet.of(
                            REFUNDED
                    )),
                    Map.entry(DELIVERED, EnumSet.noneOf(OrderStatus.class)),
                    Map.entry(REFUNDED, EnumSet.noneOf(OrderStatus.class))
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
