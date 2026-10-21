package com.lavaflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AiChatRequest(

        @NotBlank(message = "Message is required")
        @Size(
                max = 4000,
                message = "Message must not exceed 4000 characters"
        )
        String message,

        UUID conversationId

) {
}