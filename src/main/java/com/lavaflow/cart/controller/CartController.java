package com.lavaflow.cart.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.cart.dto.AddCartItemRequest;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.dto.UpdateCartItemRequest;
import com.lavaflow.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        CartResponse response = cartService.addItem(
                principal.getUser().getId(),
                request.menuItemId(),
                request.quantity()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                cartService.getCart(principal.getUser().getId())
        );
    }

    @PatchMapping("/items/{menuItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(
                cartService.updateQuantity(
                        principal.getUser().getId(),
                        menuItemId,
                        request.quantity()
                )
        );
    }

    @DeleteMapping("/items/{menuItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID menuItemId
    ) {
        return ResponseEntity.ok(
                cartService.removeItem(
                        principal.getUser().getId(),
                        menuItemId
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                cartService.clearCart(principal.getUser().getId())
        );
    }
}