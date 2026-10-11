package com.lavaflow.restaurant.listener;

import com.lavaflow.restaurant.event.RestaurantAccessRevokedEvent;
import com.lavaflow.restaurant.service.RestaurantSessionRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantAccessRevocationListener {

    private final RestaurantSessionRevocationService restaurantSessionRevocationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRevoked(RestaurantAccessRevokedEvent event) {
        try {
            restaurantSessionRevocationService.revokeAllSessions(event.restaurantId());
        } catch (Exception e) {
            log.error("Failed to revoke sessions for restaurant {}", event.restaurantId(), e);
        }
    }
}