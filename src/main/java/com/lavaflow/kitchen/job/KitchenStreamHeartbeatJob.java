package com.lavaflow.kitchen.job;

import com.lavaflow.kitchen.service.KitchenStreamRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KitchenStreamHeartbeatJob {

    private final KitchenStreamRegistry kitchenStreamRegistry;

    @Scheduled(fixedRateString = "${lavaflow.kitchen.stream.heartbeat-interval-ms:20000}")
    public void sendHeartbeat() {
        kitchenStreamRegistry.sendHeartbeat();
    }
}
