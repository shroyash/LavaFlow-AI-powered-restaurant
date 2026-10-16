package com.lavaflow.delivery.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class NearestOutletResponse {

    private String outletId;
    private String outletName;
    private String address;
    private String phone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String outletStatus;
    private boolean busy;
    private BigDecimal distanceKm;
    private int estimatedDeliveryMinutes;
}