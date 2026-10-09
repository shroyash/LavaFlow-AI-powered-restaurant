package com.lavaflow.restaurant.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RestaurantResponse {

    private String id;
    private String name;
    private String description;
    private String phone;
    private String email;
    private String logoUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}