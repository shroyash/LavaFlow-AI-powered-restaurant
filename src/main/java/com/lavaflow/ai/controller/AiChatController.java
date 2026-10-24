package com.lavaflow.ai.controller;

import com.lavaflow.ai.dto.AiChatRequest;
import com.lavaflow.ai.dto.AiChatResponse;
import com.lavaflow.ai.service.AiChatService;
import com.lavaflow.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiChatRequest request
    ) {
        AiChatService.ChatResult result = aiChatService.chat(
                principal.getUser(),
                request.conversationId(),
                request.restaurantId(),
                request.message()
        );

        return ResponseEntity.ok(
                new AiChatResponse(
                        result.conversationId(),
                        result.reply(),
                        result.restaurants().stream()
                                .map(restaurant -> new AiChatResponse.RestaurantOption(
                                        restaurant.restaurantId(),
                                        restaurant.restaurantName(),
                                        restaurant.description(),
                                        // Fall back to restaurantId when no specific outlet is found
                                        // so the frontend always has a non-null outletId for card selection.
                                        restaurant.outletId() != null
                                                ? restaurant.outletId()
                                                : restaurant.restaurantId(),
                                        restaurant.outletName(),
                                        restaurant.outletAddress(),
                                        restaurant.distanceKm() == null
                                                ? null
                                                : restaurant.distanceKm().toPlainString(),
                                        restaurant.estimatedDeliveryMinutes()
                                ))
                                .toList()
                )
        );
    }
}