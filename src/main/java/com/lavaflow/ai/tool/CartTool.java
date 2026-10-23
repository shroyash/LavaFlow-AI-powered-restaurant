
package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CartToolInput;
import com.lavaflow.ai.tool.dto.CartToolResult;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.service.CartService;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CartTool {

    private final CartService cartService;

    public CartTool(CartService cartService) {
        this.cartService = cartService;
    }

    @Tool(description = """
            Manage the authenticated customer's shopping cart.

            Supported actions:
            - view: View the current cart.
            - add: Add a menu item using its menuItemId and quantity.
            - update: Set an existing item's quantity.
            - remove: Remove a menu item.
            - clear: Remove all items from the cart.

            Use real menuItemId values from menu search results.
            Never invent menu item IDs or prices.
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
                    if (input.menuItemId() == null || input.quantity() == null) {
                        yield CartToolResult.failure(
                                "A menu item ID and quantity are required."
                        );
                    }

                    CartResponse cart = cartService.addItem(
                            customerId,
                            input.menuItemId(),
                            input.quantity()
                    );

                    yield CartToolResult.success(
                            "Item added to cart.",
                            cart
                    );
                }

                case "update" -> {
                    if (input.menuItemId() == null || input.quantity() == null) {
                        yield CartToolResult.failure(
                                "A menu item ID and new quantity are required."
                        );
                    }

                    CartResponse cart = cartService.updateQuantity(
                            customerId,
                            input.menuItemId(),
                            input.quantity()
                    );

                    yield CartToolResult.success(
                            "Cart quantity updated.",
                            cart
                    );
                }

                case "remove" -> {
                    if (input.menuItemId() == null) {
                        yield CartToolResult.failure(
                                "A menu item ID is required."
                        );
                    }

                    CartResponse cart = cartService.removeItem(
                            customerId,
                            input.menuItemId()
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
