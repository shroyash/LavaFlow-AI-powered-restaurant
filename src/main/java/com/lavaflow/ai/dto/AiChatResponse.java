package com.lavaflow.ai.dto;

import java.util.UUID;

public record AiChatResponse(
        UUID conversationId,
        String reply
) {
}