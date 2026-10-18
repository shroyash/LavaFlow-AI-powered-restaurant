package com.lavaflow.kitchen.listener;

import com.lavaflow.kitchen.service.KitchenStreamRegistry;
import com.lavaflow.restaurant.event.RestaurantAccessRevokedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class KitchenStreamLifecycleListener {

    private final KitchenStreamRegistry kitchenStreamRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRestaurantAccessRevoked(RestaurantAccessRevokedEvent event) {
        kitchenStreamRegistry.closeAll(event.restaurantId());
    }
}
