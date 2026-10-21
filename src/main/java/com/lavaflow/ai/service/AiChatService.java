package com.lavaflow.ai.service;

import com.lavaflow.ai.entity.AiConversation;
import com.lavaflow.ai.entity.AiMessage;
import com.lavaflow.ai.repository.AiConversationRepository;
import com.lavaflow.ai.repository.AiMessageRepository;
import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.ConversationStatus;
import com.lavaflow.common.enums.MessageSender;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
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

    public AiChatService(
            ChatClient.Builder builder,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;

        this.chatClient = builder
                .defaultSystem("""
                        You are LavaFlow, a restaurant ordering assistant.

                        Help customers with questions about using the
                        restaurant ordering application.

                        A restaurant has not necessarily been selected yet.
                        If the customer wants to order food without selecting
                        a restaurant, guide them to select one first.

                        Never invent restaurant names, menu items, prices,
                        food availability, or order status.

                        If menu information cannot be verified, explain that
                        the selected restaurant's menu data is required.

                        Return only the final customer-facing answer.
                        Never include internal reasoning or analysis.
                        Never include <think> or </think> tags.

                        Keep responses concise and friendly.
                        """)
                .build();
    }

    public ChatResult chat(
            User customer,
            UUID conversationId,
            String message
    ) {
        AiConversation conversation =
                getOrCreateConversation(customer, conversationId);

        saveMessage(
                conversation,
                MessageSender.CUSTOMER,
                message
        );

        String reply;

        try {
            reply = chatClient.prompt()
                    .user(message)
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

        return new ChatResult(conversation.getId(), reply);
    }

    private AiConversation getOrCreateConversation(
            User customer,
            UUID conversationId
    ) {
        if (conversationId == null) {
            AiConversation conversation = new AiConversation();
            conversation.setCustomer(customer);
            conversation.setStatus(ConversationStatus.ACTIVE);

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

        return conversation;
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

        // Remove any remaining complete reasoning blocks.
        sanitized = THINK_BLOCK_PATTERN
                .matcher(sanitized)
                .replaceAll("")
                .trim();

        String lowerCaseReply =
                sanitized.toLowerCase(Locale.ROOT);

        // Fail closed if a reasoning marker remains.
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
