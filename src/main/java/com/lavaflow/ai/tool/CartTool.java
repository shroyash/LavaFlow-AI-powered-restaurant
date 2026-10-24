
package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CartToolInput;
import com.lavaflow.ai.tool.dto.CartToolResult;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.service.CartService;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.menu.repository.MenuItemSpecifications;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CartTool {

    private final CartService cartService;
    private final MenuItemRepository menuItemRepository;

    @Autowired
    public CartTool(CartService cartService, @Autowired(required = false) MenuItemRepository menuItemRepository) {
        this.cartService = cartService;
        this.menuItemRepository = menuItemRepository;
    }

    public CartTool(CartService cartService) {
        this(cartService, null);
    }

    @Tool(description = """
            Manage the authenticated customer's shopping cart.

            Supported actions:
            - view: View the current cart.
            - add: Add a menu item using its menuItemId (or itemName) and quantity (default 1).
            - update: Set an existing item's quantity.
            - remove: Remove a menu item using its menuItemId (or itemName).
            - clear: Remove all items from the cart.

            Never invent prices or non-existent items.
            Use clear only when the customer explicitly requests it.
            The add action increases the quantity of an existing item;
            update replaces its quantity.
            """)
    public CartToolResult manageCart(
            CartToolInput input,
            ToolContext toolContext
    ) {
        UUID customerId = extractCustomerId(toolContext);

        if (customerId == null) {
            return CartToolResult.failure(
                    "Authenticated customer context is unavailable."
            );
        }

        if (input == null || input.action() == null
                || input.action().isBlank()) {
            return CartToolResult.failure(
                    "Specify an action: view, add, update, remove, or clear."
            );
        }

        try {
            return switch (input.action().trim().toLowerCase(Locale.ROOT)) {
                case "view" -> CartToolResult.success(
                        "Cart retrieved successfully.",
                        cartService.getCart(customerId)
                );

                case "add" -> {
                    int qty = (input.quantity() != null && input.quantity() > 0) ? input.quantity() : 1;
                    UUID itemId = input.menuItemId();

                    if (itemId == null && input.itemName() != null && !input.itemName().isBlank()) {
                        itemId = resolveMenuItemId(input.itemName(), toolContext);
                    }

                    if (itemId == null) {
                        yield CartToolResult.failure(
                                "A menu item ID or valid item name is required."
                        );
                    }

                    CartResponse cart = cartService.addItem(
                            customerId,
                            itemId,
                            qty
                    );

                    yield CartToolResult.success(
                            "Item added to cart.",
                            cart
                    );
                }

                case "update" -> {
                    UUID itemId = input.menuItemId();
                    if (itemId == null && input.itemName() != null && !input.itemName().isBlank()) {
                        itemId = resolveMenuItemId(input.itemName(), toolContext);
                    }

                    if (itemId == null || input.quantity() == null) {
                        yield CartToolResult.failure(
                                "A menu item ID and new quantity are required."
                        );
                    }

                    CartResponse cart = cartService.updateQuantity(
                            customerId,
                            itemId,
                            input.quantity()
                    );

                    yield CartToolResult.success(
                            "Cart quantity updated.",
                            cart
                    );
                }

                case "remove" -> {
                    UUID itemId = input.menuItemId();
                    if (itemId == null && input.itemName() != null && !input.itemName().isBlank()) {
                        itemId = resolveMenuItemId(input.itemName(), toolContext);
                    }

                    if (itemId == null) {
                        yield CartToolResult.failure(
                                "A menu item ID or valid item name is required."
                        );
                    }

                    CartResponse cart = cartService.removeItem(
                            customerId,
                            itemId
                    );

                    yield CartToolResult.success(
                            "Item removed from cart.",
                            cart
                    );
                }

                case "clear" -> CartToolResult.success(
                        "Cart cleared.",
                        cartService.clearCart(customerId)
                );

                default -> CartToolResult.failure(
                        "Unsupported action. Use view, add, update, remove, or clear."
                );
            };
        } catch (EntityNotFoundException
                 | BadRequestException
                 | ConflictException exception) {
            return CartToolResult.failure(exception.getMessage());
        }
    }

    private UUID resolveMenuItemId(String itemName, ToolContext toolContext) {
        if (menuItemRepository == null || itemName == null || itemName.isBlank()) {
            return null;
        }
        UUID restaurantId = extractRestaurantId(toolContext);
        String cleanName = itemName.trim();

        if (restaurantId != null) {
            var page = menuItemRepository.findAll(
                    MenuItemSpecifications.filter(restaurantId, null, true, cleanName),
                    PageRequest.of(0, 10)
            );
            if (page.hasContent()) {
                for (MenuItem item : page.getContent()) {
                    if (item.getName().equalsIgnoreCase(cleanName)) {
                        return item.getId();
                    }
                }
                for (MenuItem item : page.getContent()) {
                    if (item.getName().toLowerCase(Locale.ROOT).contains(cleanName.toLowerCase(Locale.ROOT))
                            || cleanName.toLowerCase(Locale.ROOT).contains(item.getName().toLowerCase(Locale.ROOT))) {
                        return item.getId();
                    }
                }
                return page.getContent().get(0).getId();
            }

            String[] words = cleanName.split("\\s+");
            for (String word : words) {
                if (word.length() > 2) {
                    var wordPage = menuItemRepository.findAll(
                            MenuItemSpecifications.filter(restaurantId, null, true, word),
                            PageRequest.of(0, 5)
                    );
                    if (wordPage.hasContent()) {
                        return wordPage.getContent().get(0).getId();
                    }
                }
            }
        }
        return null;
    }

    private UUID extractRestaurantId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();
        Object id = context.get("restaurantId");

        if (id instanceof UUID uuid) {
            return uuid;
        }

        if (id instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }

    private UUID extractCustomerId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();
        Object id = context.get("customerId");

        if (id instanceof UUID uuid) {
            return uuid;
        }

        if (id instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }
}
