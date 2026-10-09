package com.lavaflow.restaurant.repository;

import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);
}