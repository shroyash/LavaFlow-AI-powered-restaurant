package com.lavaflow.order.job;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.config.OrderProperties;
import com.lavaflow.order.repository.OrderRepository;
import com.lavaflow.order.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleOrderCancellationJob {

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;
    private final OrderProperties orderProperties;

    @Scheduled(fixedDelayString = "${lavaflow.order.stale-check-interval-ms:60000}")
    public void cancelUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(orderProperties.getUnpaidTimeoutMinutes());

        orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.CREATED, threshold).forEach(order -> {
            try {
                orderStatusService.transition(order.getId(), OrderStatus.CANCELLED);
                log.info("Cancelled unpaid order {}", order.getId());
            } catch (Exception e) {
                log.warn("Could not cancel unpaid order {}", order.getId(), e);
            }
        });
    }
}
