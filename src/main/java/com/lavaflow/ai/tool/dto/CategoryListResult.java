package com.lavaflow.ai.tool.dto;

import java.util.List;

public record CategoryListResult(
        boolean success,
        String message,
        List<MenuCategoryToolResponse> categories
) {
    public static CategoryListResult success(List<MenuCategoryToolResponse> categories) {
        return new CategoryListResult(true, "Menu categories retrieved successfully.", categories);
    }

    public static CategoryListResult noRestaurantSelected(String message) {
        return new CategoryListResult(false, message, List.of());
    }
}

