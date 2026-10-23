
package com.lavaflow.ai.service;

import com.lavaflow.ai.entity.AiConversation;
import com.lavaflow.ai.entity.AiMessage;
import com.lavaflow.ai.repository.AiConversationRepository;
import com.lavaflow.ai.repository.AiMessageRepository;
import com.lavaflow.ai.tool.CartTool;
import com.lavaflow.ai.tool.CustomerAddressTool;
import com.lavaflow.ai.tool.MenuSearchTool;
import com.lavaflow.ai.tool.NearbyRestaurantTool;
import com.lavaflow.ai.tool.RestaurantDiscoveryTool;
import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.ConversationStatus;
import com.lavaflow.common.enums.MessageSender;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AiChatService {

    private static final String FALLBACK_REPLY =
            "Sorry, I couldn't generate a response. Please try again.";

    private static final Pattern THINK_BLOCK_PATTERN =
            Pattern.compile("(?is)<think>.*?</think>");

    private final ChatClient chatClient;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final RestaurantRepository restaurantRepository;

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
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.restaurantRepository = restaurantRepository;

        this.chatClient = builder
                .defaultSystem("""
                        You are LavaFlow, a restaurant ordering assistant.

                        Help customers with questions about using the
                        restaurant ordering application.

                        You can help customers view their saved delivery addresses,
                        save new delivery addresses, find nearby restaurants and outlets that deliver
                        to their location, discover active restaurants, browse categories,
                        search menu items, and manage their shopping cart.

                        A restaurant has not necessarily been selected yet.
                        Use the nearby restaurant tool or restaurant discovery tool to help customers find restaurants.
                        If the customer wants to explore food or order without selecting
                        a restaurant, guide them to select one first.

                        Customers can manage their cart after selecting menu items.
                        Use menu search to obtain real menu item IDs and prices.
                        Never invent menu item IDs, restaurant names, menu items,
                        prices, food availability, or order status.

                        Never claim an item was added, updated, removed, or cleared
                        unless the cart tool confirms the operation succeeded.
                        Do not place orders or claim payment was completed through
                        the cart tool.

                        Return only the final customer-facing answer.
                        Never include internal reasoning or analysis.
                        Never include <think> or </think> tags.

                        Keep responses concise and friendly.
                        """)
                .defaultTools(
                        menuSearchTool,
                        restaurantDiscoveryTool,
                        customerAddressTool,
                        nearbyRestaurantTool,
                        cartTool
                )
                .build();
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
        AiConversation conversation =
                getOrCreateConversation(
                        customer,
                        conversationId,
                        requestedRestaurantId
                );

        saveMessage(
                conversation,
                MessageSender.CUSTOMER,
                message
        );

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("customerId", customer.getId());

        if (conversation.getRestaurant() != null) {
            toolContext.put(
                    "restaurantId",
                    conversation.getRestaurant().getId()
            );
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

        } catch (Exception exception) {
            // Do not expose internal exception details to customers.
            reply = FALLBACK_REPLY;
        }

        saveMessage(
                conversation,
                MessageSender.AI_ASSISTANT,
                reply
        );

        return new ChatResult(
                conversation.getId(),
                reply
        );
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

        AiConversation conversation =
                conversationRepository
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

            boolean stillActive = restaurantRepository.findById(currentId)
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

        /*
         * Some Qwen3 responses contain reasoning followed by </think>,
         * sometimes without an opening <think> marker.
         * Keep only the text after the final closing marker.
         */
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
            String reply
    ) {
    }
}
