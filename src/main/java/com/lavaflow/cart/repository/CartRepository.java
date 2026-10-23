package com.lavaflow.cart.repository;

import com.lavaflow.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerId(UUID customerId);

    @EntityGraph(attributePaths = {
            "customer",
            "restaurant",
            "items",
            "items.menuItem"
    })
    Optional<Cart> findWithItemsByCustomerId(UUID customerId);
}