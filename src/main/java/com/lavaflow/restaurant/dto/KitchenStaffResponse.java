package com.lavaflow.restaurant.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KitchenStaffResponse {

    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String restaurantId;
    private boolean active;
}