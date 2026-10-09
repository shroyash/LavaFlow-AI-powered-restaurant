package com.lavaflow.restaurant.repository;

import com.lavaflow.restaurant.entity.RestaurantDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantDocumentRepository extends JpaRepository<RestaurantDocument, UUID> {

    List<RestaurantDocument> findByRestaurantIdOrderByCreatedAtAsc(UUID restaurantId);

    Optional<RestaurantDocument> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}