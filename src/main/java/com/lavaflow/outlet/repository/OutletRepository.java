package com.lavaflow.outlet.repository;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.outlet.entity.Outlet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OutletRepository extends JpaRepository<Outlet, UUID> {

    Page<Outlet> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Page<Outlet> findByRestaurantIdAndStatus(UUID restaurantId, OutletStatus status, Pageable pageable);

    Optional<Outlet> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}