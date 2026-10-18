package com.lavaflow.kitchen.service;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.kitchen.exception.InvalidKitchenOrderStatusException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.lavaflow.common.enums.KitchenOrderStatus.ACCEPTED;
import static com.lavaflow.common.enums.KitchenOrderStatus.PENDING;
import static com.lavaflow.common.enums.KitchenOrderStatus.PREPARING;
import static com.lavaflow.common.enums.KitchenOrderStatus.READY;
import static com.lavaflow.common.enums.KitchenOrderStatus.REJECTED;

@Component
public class KitchenStateMachine {

    private static final Map<KitchenOrderStatus, Set<KitchenOrderStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ACCEPTED, REJECTED),
            ACCEPTED, EnumSet.of(PREPARING),
            PREPARING, EnumSet.of(READY)
    );

    public void requireTransition(KitchenOrderStatus from, KitchenOrderStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidKitchenOrderStatusException(from, to);
        }
    }
}
