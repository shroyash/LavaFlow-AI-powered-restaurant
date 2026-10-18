package com.lavaflow.kitchen.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class KitchenOrderResponse {

    private String id;
    private String orderId;
    private String orderNumber;
    private String status;
    private String outletId;
    private String outletName;
    private List<KitchenOrderItemResponse> items;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime preparingAt;
    private LocalDateTime readyAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
