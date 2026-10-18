package com.lavaflow.kitchen.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.kitchen.dto.StreamTicketResponse;
import com.lavaflow.kitchen.exception.InvalidStreamTicketException;
import com.lavaflow.kitchen.service.KitchenStreamRegistry;
import com.lavaflow.kitchen.service.KitchenStreamTicketService;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kitchen/stream")
@RequiredArgsConstructor
@Tag(name = "Kitchen Stream", description = "Live kitchen events over Server-Sent Events")
public class KitchenStreamController {

    private final KitchenStreamTicketService kitchenStreamTicketService;
    private final KitchenStreamRegistry kitchenStreamRegistry;
    private final CurrentRestaurantResolver currentRestaurantResolver;

    @PostMapping("/ticket")
    @PreAuthorize("hasAnyRole('KITCHEN_STAFF', 'RESTAURANT_ADMIN', 'RESTAURANT_STAFF')")
    @Operation(summary = "Get a short-lived ticket for opening the live stream")
    public ResponseEntity<ApiResponse<StreamTicketResponse>> issueTicket(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID restaurantId = currentRestaurantResolver.resolve(principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Stream ticket issued.",
                kitchenStreamTicketService.issue(restaurantId, principal.getUser().getId())
        ));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Open the live kitchen event stream using a ticket")
    public ResponseEntity<SseEmitter> stream(@RequestParam(value = "ticket", required = false) String ticket) {
        UUID restaurantId;
        try {
            restaurantId = kitchenStreamTicketService.verify(ticket);
        } catch (InvalidStreamTicketException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return kitchenStreamRegistry.register(restaurantId)
                .map(emitter -> ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                        .header("X-Accel-Buffering", "no")
                        .body(emitter))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
    }
}
