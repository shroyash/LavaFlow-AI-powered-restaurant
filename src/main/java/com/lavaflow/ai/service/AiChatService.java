
package com.lavaflow.ai.service;

import com.lavaflow.ai.entity.AiConversation;
import com.lavaflow.ai.entity.AiMessage;
import com.lavaflow.ai.repository.AiConversationRepository;
import com.lavaflow.ai.repository.AiMessageRepository;
import com.lavaflow.ai.tool.CartTool;
import com.lavaflow.ai.tool.CustomerAddressTool;
import com.lavaflow.ai.tool.MenuSearchTool;
import com.lavaflow.ai.tool.NearbyRestaurantResultCapture;
import com.lavaflow.ai.tool.NearbyRestaurantTool;
import com.lavaflow.ai.tool.OrderTool;
import com.lavaflow.ai.tool.RestaurantDiscoveryTool;
import com.lavaflow.ai.tool.dto.CustomerAddressListResult;
import com.lavaflow.ai.tool.dto.CustomerAddressToolResponse;
import com.lavaflow.ai.tool.dto.MenuItemToolResponse;
import com.lavaflow.ai.tool.dto.MenuSearchInput;
import com.lavaflow.ai.tool.dto.MenuSearchResult;
import com.lavaflow.ai.tool.dto.NearbyRestaurantToolResponse;
import com.lavaflow.ai.tool.dto.OrderListToolResult;
import com.lavaflow.ai.tool.dto.OrderStatusToolInput;
import com.lavaflow.ai.tool.dto.OrderStatusToolResult;
import com.lavaflow.ai.tool.dto.OrderSummaryToolResult;
import com.lavaflow.ai.tool.dto.PlaceOrderToolInput;
import com.lavaflow.ai.tool.dto.PlaceOrderToolResult;
import com.lavaflow.ai.tool.dto.SaveAddressInput;
import com.lavaflow.ai.tool.dto.SaveAddressResult;
import com.lavaflow.auth.entity.User;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.service.CartService;
import com.lavaflow.common.enums.ConversationStatus;
import com.lavaflow.common.enums.MessageSender;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.menu.repository.MenuItemSpecifications;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AiChatService {

    private static final String FALLBACK_REPLY =
            "Sorry, I couldn't generate a response. Please try again.";

    private static final String RESTAURANT_REQUIRED_REPLY =
            "Please select a restaurant first before I show you its menu.";

    private static final Pattern THINK_BLOCK_PATTERN =
            Pattern.compile("(?is)<think>.*?</think>");

    private static final Pattern MENU_REQUEST_PATTERN =
            Pattern.compile(
                    "(?i)\\b(menu|menulist|menu list|dishes|food items|what food|what can i order)\\b"
            );

    private static final Pattern FOOD_INQUIRY_PATTERN =
            Pattern.compile(
                    "(?i)\\b(pizza|burger|momo|momos|chicken|biryani|noodles|chowmein|pasta|fries|beverage|drink|drinks|dessert|rice|curry|soup|sandwich|salad|roll|wings|tandoori)\\b"
            );

    private static final Pattern VIEW_CART_PATTERN =
            Pattern.compile("(?i)\\b(view cart|show cart|my cart|what's in my cart|check cart|items in cart)\\b");

    private static final Pattern ORDER_SUMMARY_PATTERN =
            Pattern.compile("(?i)\\b(order summary|summary of order|checkout|check out|review order)\\b");

    private static final Pattern PLACE_ORDER_PATTERN =
            Pattern.compile("(?i)\\b(place order|place my order|confirm order|submit order)\\b");

    private static final Pattern LIST_ADDRESS_PATTERN =
            Pattern.compile("(?i)^\\s*(?:show\\s+|list\\s+|view\\s+|what(?:'s|\\s+is)\\s+)?(?:saved\\s+)?(?:my\\s+)?(?:saved\\s+)?(?:delivery\\s+)?address(?:es)?\\s*$");

    private static final Pattern SAVE_ADDRESS_PATTERN =
            Pattern.compile("(?i)^\\s*(?:please\\s+)?(?:can you\\s+)?(?:save|saved|set|add|use|update)?\\s*(?:my\\s+)?(?:delivery\\s+)?address\\s*(?:is|to|:)?\\s+(.+)$");

    private static final Pattern DELIVER_TO_PATTERN =
            Pattern.compile("(?i)^\\s*(?:please\\s+)?(?:deliver|ship)\\s+to\\s+(.+)$");

    private static final Pattern MY_ORDERS_PATTERN =
            Pattern.compile("(?i)\\b(my orders|past orders|order history|list orders)\\b");

    private static final Pattern ORDER_STATUS_PATTERN =
            Pattern.compile("(?i)\\b(order status|track order|where is my order|status of order)\\b");

    private final ChatClient chatClient;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuSearchTool menuSearchTool;
    private final CustomerAddressTool customerAddressTool;
    private final CartTool cartTool;
    private final OrderTool orderTool;
    private final CartService cartService;
    private final MenuItemRepository menuItemRepository;

    public AiChatService(
            ChatClient.Builder builder,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            RestaurantRepository restaurantRepository,
            MenuSearchTool menuSearchTool,
            RestaurantDiscoveryTool restaurantDiscoveryTool,
            CustomerAddressTool customerAddressTool,
            NearbyRestaurantTool nearbyRestaurantTool,
            CartTool cartTool
    ) {
        this(
                builder,
                conversationRepository,
                messageRepository,
                restaurantRepository,
                menuSearchTool,
                restaurantDiscoveryTool,
                customerAddressTool,
                nearbyRestaurantTool,
                cartTool,
                null,
                null,
                null
        );
    }

    public AiChatService(
            ChatClient.Builder builder,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            RestaurantRepository restaurantRepository,
            MenuSearchTool menuSearchTool,
            RestaurantDiscoveryTool restaurantDiscoveryTool,
            CustomerAddressTool customerAddressTool,
            NearbyRestaurantTool nearbyRestaurantTool,
            CartTool cartTool,
            OrderTool orderTool
    ) {
        this(
                builder,
                conversationRepository,
                messageRepository,
                restaurantRepository,
                menuSearchTool,
                restaurantDiscoveryTool,
                customerAddressTool,
                nearbyRestaurantTool,
                cartTool,
                orderTool,
                null,
                null
        );
    }

    @Autowired
    public AiChatService(
            ChatClient.Builder builder,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            RestaurantRepository restaurantRepository,
            MenuSearchTool menuSearchTool,
            RestaurantDiscoveryTool restaurantDiscoveryTool,
            CustomerAddressTool customerAddressTool,
            NearbyRestaurantTool nearbyRestaurantTool,
            CartTool cartTool,
            @Autowired(required = false) OrderTool orderTool,
            @Autowired(required = false) CartService cartService,
            @Autowired(required = false) MenuItemRepository menuItemRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuSearchTool = menuSearchTool;
        this.customerAddressTool = customerAddressTool;
        this.cartTool = cartTool;
        this.orderTool = orderTool;
        this.cartService = cartService;
        this.menuItemRepository = menuItemRepository;

        var clientSpec = builder
                .defaultSystem("""
                        You are LavaFlow, an AI restaurant ordering assistant.

                        TRUSTED RESTAURANT CONTEXT:
                        The backend may provide restaurantId and
                        selectedRestaurantName. These values are authoritative.
                        Never invent or change them.

                        RESTAURANT SELECTION:
                        - If a restaurant is selected, continue with it.
                        - Do not ask the customer to select it again.
                        - A restaurant mentioned in text is not necessarily selected.
                        - Never call restaurant discovery just because the customer
                          says "yes", "okay", or "sure".
                        - If no restaurant is selected, do not search its menu.

                        TOOL RULES:
                        - Nearby restaurant requests: use the appropriate discovery tool.
                        - Menu requests: call searchMenu for the selected restaurant.
                        - Cart requests: use the cart tool (manageCart).
                        - Saved address requests: use the address tool.
                        - Order review/summary requests: use getOrderSummary.
                        - Place order or checkout requests: use placeOrder.
                        - Order status and history requests: use getMyOrders or getOrderStatus.
                        - Never invent restaurant names, menu items, prices,
                          IDs, or availability.
                        - Never claim an action succeeded unless a tool confirms it.

                        Keep responses concise and friendly.
                        Never output <think> tags.
                        """);

        if (orderTool != null) {
            clientSpec.defaultTools(
                    menuSearchTool,
                    restaurantDiscoveryTool,
                    customerAddressTool,
                    nearbyRestaurantTool,
                    cartTool,
                    orderTool
            );
        } else {
            clientSpec.defaultTools(
                    menuSearchTool,
                    restaurantDiscoveryTool,
                    customerAddressTool,
                    nearbyRestaurantTool,
                    cartTool
            );
        }

        this.chatClient = clientSpec.build();
    }

    public ChatResult chat(
            User customer,
            UUID conversationId,
            String message
    ) {
        return chat(customer, conversationId, null, message);
    }

    public ChatResult chat(
            User customer,
            UUID conversationId,
            UUID requestedRestaurantId,
            String message
    ) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message must not be blank.");
        }

        AiConversation conversation = getOrCreateConversation(
                customer,
                conversationId,
                requestedRestaurantId
        );

        saveMessage(conversation, MessageSender.CUSTOMER, message);

        Restaurant selectedRestaurant = conversation.getRestaurant();

        /*
         * Enforce the selection rule in backend code.
         * Do not rely on the language model to enforce it.
         */
        if (isMenuRequest(message) && selectedRestaurant == null) {
            String reply = RESTAURANT_REQUIRED_REPLY;

            saveMessage(
                    conversation,
                    MessageSender.AI_ASSISTANT,
                    reply
            );

            return new ChatResult(
                    conversation.getId(),
                    reply,
                    List.of()
            );
        }

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("customerId", customer.getId());

        NearbyRestaurantResultCapture restaurantCapture =
                new NearbyRestaurantResultCapture();

        toolContext.put(
                "nearbyRestaurantResultCapture",
                restaurantCapture
        );

        if (selectedRestaurant != null) {
            toolContext.put(
                    "restaurantId",
                    selectedRestaurant.getId()
            );

            toolContext.put(
                    "selectedRestaurantName",
                    selectedRestaurant.getName()
            );
        }

        // 1. Check for Add to Cart intent
        AddToCartIntent cartIntent = parseAddToCartIntent(message, selectedRestaurant != null ? selectedRestaurant.getId() : null);
        if (cartIntent != null) {
            if (selectedRestaurant == null) {
                String reply = "Please select a restaurant first before adding items to your cart.";
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            }

            if (cartService != null && menuItemRepository != null) {
                MenuItem item = findMenuItem(selectedRestaurant.getId(), cartIntent.itemName());
                if (item != null) {
                    try {
                        CartResponse updatedCart = cartService.addItem(customer.getId(), item.getId(), cartIntent.quantity());
                        String reply = String.format(
                                "Added **%s** (Qty: %d) to your cart! Your current total is Rs. %.2f. Would you like to add anything else or proceed to checkout?",
                                item.getName(), cartIntent.quantity(), updatedCart.subtotal()
                        );
                        saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                        return new ChatResult(conversation.getId(), reply, List.of());
                    } catch (Exception ex) {
                        log.warn("Direct cart addition error: {}", ex.getMessage());
                    }
                }
            }
        }

        // 2. Check for View Cart intent
        if (VIEW_CART_PATTERN.matcher(message).find() && cartService != null) {
            try {
                CartResponse cart = cartService.getCart(customer.getId());
                if (cart.items() == null || cart.items().isEmpty()) {
                    String reply = "Your cart is currently empty. Would you like to view the menu or explore restaurants?";
                    saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                    return new ChatResult(conversation.getId(), reply, List.of());
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Here is your cart from **").append(cart.restaurantName()).append("**:\n\n");
                for (CartResponse.CartItemResponse itm : cart.items()) {
                    sb.append("- **").append(itm.name()).append("** x ").append(itm.quantity())
                            .append(": Rs. ").append(itm.lineTotal()).append("\n");
                }
                sb.append("\n**Total**: Rs. ").append(cart.subtotal());
                sb.append("\n\nWould you like to add more items or proceed to checkout?");
                String reply = sb.toString();
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            } catch (EntityNotFoundException ignored) {
                String reply = "Your cart is currently empty. Would you like to view the menu or explore restaurants?";
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            }
        }

        // 3. Check for Order Summary / Checkout intent
        if (ORDER_SUMMARY_PATTERN.matcher(message).find() && orderTool != null) {
            OrderSummaryToolResult summary = orderTool.getOrderSummary(new ToolContext(toolContext));
            if (summary.success()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Here is your order summary for **").append(summary.restaurantName()).append("**:\n\n");
                for (OrderSummaryToolResult.CartItemSummary itm : summary.items()) {
                    sb.append("- **").append(itm.name()).append("** x ").append(itm.quantity())
                            .append(": Rs. ").append(itm.lineTotal()).append("\n");
                }
                sb.append("\n**Total**: Rs. ").append(summary.subtotal());
                if (summary.deliveryAddress() != null) {
                    sb.append("\n**Delivery Address**: ").append(summary.deliveryAddress());
                } else {
                    sb.append("\n**Delivery Address**: None saved yet. Please specify your delivery address.");
                }
                sb.append("\n**Payment Method**: Cash on Delivery\n\n");
                if (summary.readyToPlace()) {
                    sb.append("Ready to checkout! Reply with **place order** to confirm.");
                } else {
                    sb.append("Please specify your delivery address so we can complete your order.");
                }
                String reply = sb.toString();
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            } else {
                String reply = summary.message();
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            }
        }

        // 4. Check for Place Order intent
        if (PLACE_ORDER_PATTERN.matcher(message).find() && orderTool != null) {
            PlaceOrderToolResult orderResult = orderTool.placeOrder(
                    new PlaceOrderToolInput(),
                    new ToolContext(toolContext)
            );
            if (orderResult.success()) {
                String reply = String.format(
                        "🎉 Your order **#%s** has been placed successfully with **%s**!\n\n- **Total Amount**: Rs. %.2f\n- **Delivery Address**: %s\n- **Payment Method**: %s\n- **Status**: %s\n\nThank you for ordering with LavaFlow!",
                        orderResult.orderNumber(),
                        orderResult.restaurantName(),
                        orderResult.totalAmount(),
                        orderResult.deliveryAddress(),
                        orderResult.paymentMethod(),
                        orderResult.status()
                );
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            } else {
                String reply = orderResult.message();
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            }
        }

        // 5. Check for Save Address intent
        if (customerAddressTool != null) {
            String extractedAddress = extractSaveAddressText(message);
            if (extractedAddress != null) {
                SaveAddressResult result = customerAddressTool.saveDeliveryAddress(
                        new SaveAddressInput(extractedAddress, null),
                        new ToolContext(toolContext)
                );
                String reply;
                if (result.success()) {
                    reply = String.format(
                            "✅ Your delivery address **\"%s\"** has been saved! Would you like to proceed to order summary or add more items?",
                            result.address().addressLine()
                    );
                } else {
                    reply = "I wasn't able to save that address: " + result.message() + " Please try again with your full delivery address.";
                }
                saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
                return new ChatResult(conversation.getId(), reply, List.of());
            }
        }

        // 6. Check for List Saved Addresses intent
        if (customerAddressTool != null && LIST_ADDRESS_PATTERN.matcher(message.trim()).matches()) {
            CustomerAddressListResult listResult = customerAddressTool.listSavedAddresses(
                    new ToolContext(toolContext)
            );
            String reply;
            if (listResult.success() && listResult.addresses() != null && !listResult.addresses().isEmpty()) {
                StringBuilder sb = new StringBuilder("Here are your saved delivery addresses:\n\n");
                for (CustomerAddressToolResponse addr : listResult.addresses()) {
                    sb.append("- **").append(addr.addressLine()).append("**");
                    if (addr.label() != null && !addr.label().isBlank()) {
                        sb.append(" (").append(addr.label()).append(")");
                    }
                    if (addr.defaultAddress()) {
                        sb.append(" ✓ *default*");
                    }
                    sb.append("\n");
                }
                sb.append("\nWould you like to place an order or update your address?");
                reply = sb.toString();
            } else if (listResult.success()) {
                reply = "You don't have any saved addresses yet. Please share your delivery address and I'll save it for you!";
            } else {
                reply = listResult.message();
            }
            saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
            return new ChatResult(conversation.getId(), reply, List.of());
        }

        // 7. Check for My Orders intent
        if (MY_ORDERS_PATTERN.matcher(message).find() && orderTool != null) {
            OrderListToolResult ordersResult = orderTool.getMyOrders(new ToolContext(toolContext));
            String reply;
            if (ordersResult.success() && ordersResult.orders() != null && !ordersResult.orders().isEmpty()) {
                StringBuilder sb = new StringBuilder("Here are your recent orders:\n\n");
                for (OrderListToolResult.OrderSummaryItem ord : ordersResult.orders()) {
                    sb.append("- **#").append(ord.orderNumber()).append("** at *").append(ord.restaurantName())
                            .append("* — Rs. ").append(ord.totalAmount())
                            .append(" — Status: **").append(ord.status()).append("**\n");
                }
                reply = sb.toString().trim();
            } else if (ordersResult.success()) {
                reply = "You haven't placed any orders yet. Would you like to explore restaurants and order something?";
            } else {
                reply = ordersResult.message();
            }
            saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
            return new ChatResult(conversation.getId(), reply, List.of());
        }

        // 8. Check for Order Status intent
        if (ORDER_STATUS_PATTERN.matcher(message).find() && orderTool != null) {
            OrderStatusToolResult statusResult = orderTool.getOrderStatus(
                    new OrderStatusToolInput(null),
                    new ToolContext(toolContext)
            );
            String reply;
            if (statusResult.success()) {
                reply = String.format(
                        "Your order **#%s** is currently **%s**.\n\n- **Restaurant**: %s\n- **Delivery Address**: %s\n- **Total**: Rs. %.2f",
                        statusResult.orderNumber(),
                        statusResult.status(),
                        statusResult.restaurantName(),
                        statusResult.deliveryAddress() != null ? statusResult.deliveryAddress() : "N/A",
                        statusResult.totalAmount()
                );
            } else {
                reply = statusResult.message();
            }
            saveMessage(conversation, MessageSender.AI_ASSISTANT, reply);
            return new ChatResult(conversation.getId(), reply, List.of());
        }

        String reply;

        try {
            reply = chatClient.prompt()
                    .user(message)
                    .toolContext(toolContext)
                    .options(
                            OllamaChatOptions.builder()
                                    .disableThinking()
                    )
                    .call()
                    .content();

            reply = sanitizeReply(reply);

            if (selectedRestaurant != null && (reply == null || !reply.contains("Rs."))) {
                if (isMenuRequest(message)) {
                    reply = enrichReplyWithMenu(reply, selectedRestaurant, "all", toolContext);
                } else if (isFoodInquiry(message)) {
                    reply = enrichReplyWithMenu(reply, selectedRestaurant, message, toolContext);
                }
            }
        } catch (Exception exception) {
            log.error(
                    "AI chat generation error for conversation {}",
                    conversation.getId(),
                    exception
            );

            reply = FALLBACK_REPLY;
        }

        saveMessage(
                conversation,
                MessageSender.AI_ASSISTANT,
                reply
        );

        return new ChatResult(
                conversation.getId(),
                reply,
                restaurantCapture.getRestaurants()
        );
    }

    private record AddToCartIntent(String itemName, int quantity) {}

    /**
     * Extracts the address text from save-address phrasings.
     * Returns null if the message doesn't look like an address-save request.
     * Examples handled:
     *   "save my delivery address patan"
     *   "saved my delivery address Patan"
     *   "my address is Lalitpur"
     *   "deliver to Kathmandu"
     *   "set address to Bhaktapur, Nepal"
     */
    private String extractSaveAddressText(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();

        // Pattern: "save[d]? [my] [delivery] address [to|is]? ADDRESS"
        Matcher m1 = SAVE_ADDRESS_PATTERN.matcher(trimmed);
        if (m1.matches()) {
            String addr = m1.group(1);
            if (addr != null && !addr.isBlank() && addr.trim().length() > 1) {
                return addr.trim();
            }
        }

        // Pattern: "deliver[y] to ADDRESS"
        Matcher m2 = DELIVER_TO_PATTERN.matcher(trimmed);
        if (m2.matches()) {
            String addr = m2.group(1);
            if (addr != null && !addr.isBlank() && addr.trim().length() > 1) {
                return addr.trim();
            }
        }

        return null;
    }

    private AddToCartIntent parseAddToCartIntent(String message, UUID restaurantId) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();

        // 1. Match "add [qty] [item] to [my] cart" or "add [qty] [item] to [my] order"
        Matcher matcherWithCart = Pattern.compile(
                "(?i)^\\s*(?:please\\s+)?(?:can you\\s+)?add\\s+(?:(\\d+)\\s+)?(.+?)(?:\\s+to\\s+(?:my\\s+)?(?:cart|order))\\s*$"
        ).matcher(trimmed);
        if (matcherWithCart.matches()) {
            String qtyStr = matcherWithCart.group(1);
            int qty = (qtyStr != null && !qtyStr.isBlank()) ? Integer.parseInt(qtyStr) : 1;
            String name = matcherWithCart.group(2).trim();
            return new AddToCartIntent(name, qty);
        }

        // 2. Match simple "add [qty] [item]" if it matches a known menu item in the selected restaurant
        if (restaurantId != null && menuItemRepository != null) {
            Matcher matcherSimpleAdd = Pattern.compile(
                    "(?i)^\\s*(?:please\\s+)?(?:can you\\s+)?add\\s+(?:(\\d+)\\s+)?(.+?)\\s*$"
            ).matcher(trimmed);
            if (matcherSimpleAdd.matches()) {
                String qtyStr = matcherSimpleAdd.group(1);
                int qty = (qtyStr != null && !qtyStr.isBlank()) ? Integer.parseInt(qtyStr) : 1;
                String candidateName = matcherSimpleAdd.group(2).trim();
                MenuItem item = findMenuItem(restaurantId, candidateName);
                if (item != null) {
                    return new AddToCartIntent(candidateName, qty);
                }
            }
        }

        return null;
    }

    private MenuItem findMenuItem(UUID restaurantId, String name) {
        if (menuItemRepository == null || restaurantId == null || name == null || name.isBlank()) {
            return null;
        }
        String clean = name.trim();
        clean = clean.replaceAll("(?i)\\b(to\\s+(my\\s+)?(?:cart|order))\\b", "").trim();

        var page = menuItemRepository.findAll(
                MenuItemSpecifications.filter(restaurantId, null, true, clean),
                PageRequest.of(0, 10)
        );
        if (page.hasContent()) {
            for (MenuItem item : page.getContent()) {
                if (item.getName().equalsIgnoreCase(clean)) {
                    return item;
                }
            }
            for (MenuItem item : page.getContent()) {
                if (item.getName().toLowerCase(Locale.ROOT).contains(clean.toLowerCase(Locale.ROOT))
                        || clean.toLowerCase(Locale.ROOT).contains(item.getName().toLowerCase(Locale.ROOT))) {
                    return item;
                }
            }
            return page.getContent().get(0);
        }

        String[] words = clean.split("\\s+");
        for (String word : words) {
            if (word.length() > 2) {
                var wordPage = menuItemRepository.findAll(
                        MenuItemSpecifications.filter(restaurantId, null, true, word),
                        PageRequest.of(0, 5)
                );
                if (wordPage.hasContent()) {
                    return wordPage.getContent().get(0);
                }
            }
        }
        return null;
    }

    private boolean isMenuRequest(String message) {
        return MENU_REQUEST_PATTERN.matcher(message).find();
    }

    private boolean isFoodInquiry(String message) {
        return FOOD_INQUIRY_PATTERN.matcher(message).find();
    }

    private String enrichReplyWithMenu(
            String baseReply,
            Restaurant restaurant,
            String query,
            Map<String, Object> toolContext
    ) {
        try {
            MenuSearchResult result = menuSearchTool.searchMenu(
                    new MenuSearchInput(query),
                    new ToolContext(toolContext)
            );
            if (result != null && result.success() && result.items() != null && !result.items().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (baseReply != null && !baseReply.isBlank() && !baseReply.equals(FALLBACK_REPLY)) {
                    sb.append(baseReply).append("\n\n");
                }
                sb.append("Here is the menu for **").append(restaurant.getName()).append("**:\n");
                for (MenuItemToolResponse item : result.items()) {
                    sb.append("- **").append(item.name()).append("**: Rs. ").append(item.price());
                    if (item.description() != null && !item.description().isBlank()) {
                        sb.append(" - ").append(item.description());
                    }
                    sb.append("\n");
                }
                return sb.toString().trim();
            }
        } catch (Exception ignored) {
        }
        return baseReply;
    }

    private AiConversation getOrCreateConversation(
            User customer,
            UUID conversationId,
            UUID requestedRestaurantId
    ) {
        Restaurant validatedRestaurant =
                validateRestaurantIfProvided(requestedRestaurantId);

        if (conversationId == null) {
            AiConversation conversation = new AiConversation();
            conversation.setCustomer(customer);
            conversation.setStatus(ConversationStatus.ACTIVE);

            if (validatedRestaurant != null) {
                conversation.setRestaurant(validatedRestaurant);
            }

            return conversationRepository.save(conversation);
        }

        AiConversation conversation = conversationRepository
                .findByIdAndCustomer_Id(
                        conversationId,
                        customer.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Conversation not found."
                        )
                );

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This conversation is no longer active."
            );
        }

        if (validatedRestaurant != null) {
            Restaurant currentRestaurant = conversation.getRestaurant();

            if (currentRestaurant == null
                    || !currentRestaurant.getId()
                    .equals(validatedRestaurant.getId())) {
                conversation.setRestaurant(validatedRestaurant);
                conversation = conversationRepository.save(conversation);
            }
        } else if (conversation.getRestaurant() != null) {
            UUID currentId = conversation.getRestaurant().getId();

            boolean stillActive = restaurantRepository
                    .findById(currentId)
                    .map(r -> r.getStatus() == RestaurantStatus.ACTIVE)
                    .orElse(false);

            if (!stillActive) {
                conversation.setRestaurant(null);
                conversation = conversationRepository.save(conversation);
            }
        }

        return conversation;
    }

    private Restaurant validateRestaurantIfProvided(
            UUID requestedRestaurantId
    ) {
        if (requestedRestaurantId == null) {
            return null;
        }

        Restaurant restaurant = restaurantRepository
                .findById(requestedRestaurantId)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Selected restaurant does not exist."
                        )
                );

        if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new BadRequestException(
                    "Selected restaurant is currently inactive."
            );
        }

        return restaurant;
    }

    private void saveMessage(
            AiConversation conversation,
            MessageSender sender,
            String content
    ) {
        AiMessage message = new AiMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);

        messageRepository.save(message);
    }

    private String sanitizeReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return FALLBACK_REPLY;
        }

        String sanitized = reply.trim();

        String closingTag = "</think>";

        int closingIndex = sanitized
                .toLowerCase(Locale.ROOT)
                .lastIndexOf(closingTag);

        if (closingIndex >= 0) {
            sanitized = sanitized
                    .substring(closingIndex + closingTag.length())
                    .trim();
        }

        sanitized = THINK_BLOCK_PATTERN
                .matcher(sanitized)
                .replaceAll("")
                .trim();

        String lowerCaseReply =
                sanitized.toLowerCase(Locale.ROOT);

        if (lowerCaseReply.contains("<think>")
                || lowerCaseReply.contains("</think>")) {
            return FALLBACK_REPLY;
        }

        return sanitized.isBlank()
                ? FALLBACK_REPLY
                : sanitized;
    }

    public record ChatResult(
            UUID conversationId,
            String reply,
            List<NearbyRestaurantToolResponse> restaurants
    ) {
        public ChatResult(UUID conversationId, String reply) {
            this(conversationId, reply, List.of());
        }
    }
}
