package com.lavaflow.outlet.repository;

import com.lavaflow.common.enums.RestaurantStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.outlet.entity.Outlet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutletRepository extends JpaRepository<Outlet, UUID> {

    Page<Outlet> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Page<Outlet> findByRestaurantIdAndStatus(UUID restaurantId, OutletStatus status, Pageable pageable);

    Optional<Outlet> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    List<Outlet> findByRestaurantIdAndStatusIn(UUID restaurantId, Collection<OutletStatus> statuses);

    @Query("""
            select o from Outlet o join fetch o.restaurant r
            where r.status = :restaurantStatus and o.status in :outletStatuses
              and o.latitude is not null and o.longitude is not null
            """)
    List<Outlet> findDeliverableOutlets(
            @Param("restaurantStatus") RestaurantStatus restaurantStatus,
            @Param("outletStatuses") Collection<OutletStatus> outletStatuses
    );
}