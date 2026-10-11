package com.lavaflow.menu.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MenuItemResponse {

    private String id;
    private String restaurantId;
    private String categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}