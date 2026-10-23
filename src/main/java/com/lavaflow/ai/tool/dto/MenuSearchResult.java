package com.lavaflow.ai.tool.dto;

import java.util.List;

public record MenuSearchResult(
        boolean success,
        String message,
        List<MenuItemToolResponse> items
) {
    public static MenuSearchResult success(List<MenuItemToolResponse> items) {
        return new MenuSearchResult(true, "Menu items retrieved successfully.", items);
    }

    public static MenuSearchResult noRestaurantSelected(String message) {
        return new MenuSearchResult(false, message, List.of());
    }

    public static MenuSearchResult validationError(String message) {
        return new MenuSearchResult(false, message, List.of());
    }
}

