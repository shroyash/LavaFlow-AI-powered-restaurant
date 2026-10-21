package com.lavaflow.ai.tool;

import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.service.MenuItemService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MenuSearchTool {

    private final MenuItemService menuItemService;

    public MenuSearchTool(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @Tool(description = """
            Search the menu of the currently selected restaurant.
            Use this when a customer asks about menu items, food names,
            prices, or available dishes.
            Only return menu items actually present in the database.
            """)
    public List<MenuItemResponse> searchMenu(
            UUID restaurantId,
            String query) {

        return menuItemService.getItems(
                        restaurantId,
                        null,
                        true,
                        query,
                        PageRequest.of(0, 10))
                .getContent();
    }
}