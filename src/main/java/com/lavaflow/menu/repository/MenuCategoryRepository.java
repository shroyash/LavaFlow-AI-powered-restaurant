package com.lavaflow.menu.repository;

import com.lavaflow.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAscNameAsc(UUID restaurantId);

    List<MenuCategory> findByRestaurantIdAndActiveOrderByDisplayOrderAscNameAsc(UUID restaurantId, boolean active);

    Optional<MenuCategory> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id);

    @Query("select coalesce(max(c.displayOrder), 0) from MenuCategory c where c.restaurant.id = :restaurantId")
    int findMaxDisplayOrder(@Param("restaurantId") UUID restaurantId);
}