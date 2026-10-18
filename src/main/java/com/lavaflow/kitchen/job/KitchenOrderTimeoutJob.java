package com.lavaflow.kitchen.job;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.kitchen.config.KitchenProperties;
import com.lavaflow.kitchen.repository.KitchenOrderRepository;
import com.lavaflow.kitchen.service.KitchenOrderActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class KitchenOrderTimeoutJob {

    private final KitchenOrderRepository kitchenOrderRepository;
    private final KitchenOrderActionService kitchenOrderActionService;
    private final KitchenProperties kitchenProperties;

    @Scheduled(fixedDelayString = "${lavaflow.kitchen.timeout-check-interval-ms:60000}")
    public void rejectUnansweredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(kitchenProperties.getAcceptTimeoutMinutes());

        kitchenOrderRepository.findByStatusAndCreatedAtBefore(KitchenOrderStatus.PENDING, threshold)
                .forEach(kitchenOrder -> {
                    try {
                        kitchenOrderActionService.autoReject(kitchenOrder.getId());
                        log.info("Auto-rejected unanswered kitchen order {}", kitchenOrder.getId());
                    } catch (Exception e) {
                        log.warn("Could not auto-reject kitchen order {}", kitchenOrder.getId(), e);
                    }
                });
    }
}
