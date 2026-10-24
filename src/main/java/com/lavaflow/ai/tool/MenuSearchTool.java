package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.MenuItemToolResponse;
import com.lavaflow.ai.tool.dto.MenuSearchInput;
import com.lavaflow.ai.tool.dto.MenuSearchResult;
import com.lavaflow.menu.service.MenuItemService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MenuSearchTool {

    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_RESULTS = 10;

    private final MenuItemService menuItemService;

    public MenuSearchTool(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @Tool(description = """
            Search dishes and food items in the currently selected restaurant's menu.
            Use this when a customer asks about specific dishes, menu items, prices, or recommendations.
            A restaurant must already be selected for this conversation.
            """)
    public MenuSearchResult searchMenu(
            MenuSearchInput input,
            ToolContext toolContext
    ) {
        UUID restaurantId = extractRestaurantId(toolContext);
        if (restaurantId == null) {
            return MenuSearchResult.noRestaurantSelected(
                    "No restaurant is currently selected. Please select a restaurant first before searching the menu."
            );
        }

        if (input == null || input.query() == null || input.query().isBlank()) {
            return MenuSearchResult.validationError("Search query must not be blank.");
        }

        String trimmedQuery = input.query().trim();
        if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
            return MenuSearchResult.validationError(
                    "Search query exceeds the maximum allowed length of " + MAX_QUERY_LENGTH + " characters."
            );
        }

        String searchPattern = isGeneralMenuQuery(trimmedQuery) ? null : trimmedQuery;

        List<MenuItemToolResponse> items = menuItemService.getItems(
                        restaurantId,
                        null,
                        true,
                        searchPattern,
                        PageRequest.of(0, MAX_RESULTS))
                .getContent()
                .stream()
                .map(item -> new MenuItemToolResponse(
                        UUID.fromString(item.getId()),
                        item.getName(),
                        item.getCategoryName(),
                        item.getDescription(),
                        item.getPrice(),
                        item.isAvailable()
                ))
                .toList();

        if (items.isEmpty() && searchPattern != null) {
            var fallbackPage = menuItemService.getItems(
                            restaurantId,
                            null,
                            true,
                            null,
                            PageRequest.of(0, MAX_RESULTS));
            if (fallbackPage != null && fallbackPage.getContent() != null && !fallbackPage.getContent().isEmpty()) {
                items = fallbackPage.getContent()
                        .stream()
                        .map(item -> new MenuItemToolResponse(
                                UUID.fromString(item.getId()),
                                item.getName(),
                                item.getCategoryName(),
                                item.getDescription(),
                                item.getPrice(),
                                item.isAvailable()
                        ))
                        .toList();
            }
        }

        return MenuSearchResult.success(items);
    }

    private boolean isGeneralMenuQuery(String q) {
        String lower = q.toLowerCase(java.util.Locale.ROOT).trim();
        return lower.equals("all") || lower.equals("menu") || lower.equals("full menu")
                || lower.equals("menulist") || lower.equals("menu list")
                || lower.equals("dishes") || lower.equals("food") || lower.equals("*");
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
