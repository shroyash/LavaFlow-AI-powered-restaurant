package com.lavaflow.delivery.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class NearbyRestaurantResponse {

    private String restaurantId;
    private String restaurantName;
    private String description;
    private String logoUrl;
    private String outletId;
    private String outletName;
    private String outletAddress;
    private boolean busy;
    private BigDecimal distanceKm;
    private int estimatedDeliveryMinutes;
}