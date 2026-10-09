package com.lavaflow.restaurant.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterRestaurantResponse {

    private String restaurantId;
    private String restaurantName;
    private String restaurantStatus;
    private String adminUserId;
    private String adminEmail;
}