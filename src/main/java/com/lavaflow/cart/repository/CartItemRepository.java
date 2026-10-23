package com.lavaflow.cart.repository;

import com.lavaflow.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndMenuItemId(
            UUID cartId,
            UUID menuItemId
    );

    Optional<CartItem> findByIdAndCartId(
            UUID cartItemId,
            UUID cartId
    );

    boolean existsByCartId(UUID cartId);

    void deleteByCartId(UUID cartId);
}