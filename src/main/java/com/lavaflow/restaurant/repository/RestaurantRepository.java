package com.lavaflow.restaurant.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    @Query("select r.id from Restaurant r where r.status = :status and lower(r.name) like :pattern")
    List<UUID> findIdsByNamePattern(@Param("status") RestaurantStatus status, @Param("pattern") String pattern);
}