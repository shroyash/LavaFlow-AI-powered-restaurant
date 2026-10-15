package com.lavaflow.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderSummaryResponse {

    private String id;
    private String orderNumber;
    private String status;
    private String restaurantName;
    private String outletName;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private LocalDateTime createdAt;
}
