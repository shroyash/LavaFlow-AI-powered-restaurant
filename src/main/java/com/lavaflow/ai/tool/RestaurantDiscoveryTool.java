package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CategoryListResult;
import com.lavaflow.ai.tool.dto.MenuCategoryToolResponse;
import com.lavaflow.ai.tool.dto.RestaurantToolResponse;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.menu.service.MenuCategoryService;
import com.lavaflow.restaurant.service.RestaurantService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RestaurantDiscoveryTool {

    private final RestaurantService restaurantService;
    private final MenuCategoryService menuCategoryService;

    public RestaurantDiscoveryTool(
            RestaurantService restaurantService,
            MenuCategoryService menuCategoryService
    ) {
        this.restaurantService = restaurantService;
        this.menuCategoryService = menuCategoryService;
    }

    @Tool(description = """
            List active restaurants available for ordering on LavaFlow.
            Use this when the customer wants to explore, discover, or find available restaurants.
            These restaurants are currently active on the platform.
            """)
    public List<RestaurantToolResponse> listAvailableRestaurants() {
        return restaurantService.getRestaurants(
                        RestaurantStatus.ACTIVE,
                        PageRequest.of(0, 10, Sort.by("name").ascending())
                )
                .getContent()
                .stream()
                .map(r -> new RestaurantToolResponse(
                        UUID.fromString(r.getId()),
                        r.getName(),
                        r.getDescription(),
                        "ACTIVE"
                ))
                .toList();
    }

    @Tool(description = """
            List menu categories for the customer's currently selected restaurant.
            Use this when a customer asks about categories, food sections, or types of dishes.
            A restaurant must already be selected for this conversation.
            """)
    public CategoryListResult getMenuCategories(ToolContext toolContext) {
        UUID restaurantId = extractRestaurantId(toolContext);
        if (restaurantId == null) {
            return CategoryListResult.noRestaurantSelected(
                    "No restaurant is currently selected. Please select a restaurant first to view its menu categories."
            );
        }

        List<MenuCategoryToolResponse> categories = menuCategoryService
                .getCategories(restaurantId, true)
                .stream()
                .map(c -> new MenuCategoryToolResponse(c.getName(), c.getDescription()))
                .toList();

        return CategoryListResult.success(categories);
    }

    private UUID extractRestaurantId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object idObj = toolContext.getContext().get("restaurantId");
        if (idObj instanceof UUID uuid) {
            return uuid;
        } else if (idObj instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}

