package com.lavaflow.menu.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.lavaflow.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID>, JpaSpecificationExecutor<MenuItem> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<MenuItem> findAll(Specification<MenuItem> specification, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByCategoryIdAndNameIgnoreCase(UUID categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(UUID categoryId, String name, UUID id);

    @Query("select distinct m.restaurant.id from MenuItem m where m.available = true and lower(m.name) like :pattern")
    List<UUID> findRestaurantIdsByAvailableItemName(@Param("pattern") String pattern);

    @EntityGraph(attributePaths = "category")
    List<MenuItem> findByRestaurantIdAndIdIn(
            UUID restaurantId,
            List<UUID> ids
    );
}