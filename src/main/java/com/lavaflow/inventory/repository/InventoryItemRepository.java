package com.lavaflow.inventory.repository;

import com.lavaflow.inventory.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, UUID>, JpaSpecificationExecutor<InventoryItem> {

    @Override
    @EntityGraph(attributePaths = {"outlet", "menuItem"})
    Page<InventoryItem> findAll(Specification<InventoryItem> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"outlet", "menuItem"})
    Optional<InventoryItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByOutletIdAndMenuItemId(UUID outletId, UUID menuItemId);

    @Query("""
            select i.quantity - i.reservedQuantity
            from InventoryItem i
            where i.outlet.id = :outletId and i.menuItem.id = :menuItemId
            """)
    Optional<Integer> findAvailableQuantity(
            @Param("outletId") UUID outletId,
            @Param("menuItemId") UUID menuItemId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update InventoryItem i
            set i.quantity = :quantity, i.lowStockThreshold = :threshold, i.updatedAt = :now
            where i.id = :id and i.restaurant.id = :restaurantId and i.reservedQuantity <= :quantity
            """)
    int updateStock(
            @Param("id") UUID id,
            @Param("restaurantId") UUID restaurantId,
            @Param("quantity") int quantity,
            @Param("threshold") int threshold,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update InventoryItem i
            set i.quantity = i.quantity + :delta, i.updatedAt = :now
            where i.id = :id and i.restaurant.id = :restaurantId
              and i.quantity + :delta >= i.reservedQuantity
            """)
    int adjustQuantity(
            @Param("id") UUID id,
            @Param("restaurantId") UUID restaurantId,
            @Param("delta") int delta,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from InventoryItem i
            where i.id = :id and i.restaurant.id = :restaurantId and i.reservedQuantity = 0
            """)
    int deleteUnreserved(@Param("id") UUID id, @Param("restaurantId") UUID restaurantId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update InventoryItem i
            set i.reservedQuantity = i.reservedQuantity + :quantity, i.updatedAt = :now
            where i.outlet.id = :outletId and i.menuItem.id = :menuItemId
              and (i.quantity - i.reservedQuantity) >= :quantity
            """)
    int reserve(
            @Param("outletId") UUID outletId,
            @Param("menuItemId") UUID menuItemId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update InventoryItem i
            set i.reservedQuantity = i.reservedQuantity - :quantity, i.updatedAt = :now
            where i.outlet.id = :outletId and i.menuItem.id = :menuItemId
              and i.reservedQuantity >= :quantity
            """)
    int release(
            @Param("outletId") UUID outletId,
            @Param("menuItemId") UUID menuItemId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );


    @Modifying(flushAutomatically = true)
    @Query("""
            update InventoryItem i
            set i.quantity = i.quantity - :quantity,
                i.reservedQuantity = i.reservedQuantity - :quantity,
                i.updatedAt = :now
            where i.outlet.id = :outletId and i.menuItem.id = :menuItemId
              and i.reservedQuantity >= :quantity
            """)
    int consume(
            @Param("outletId") UUID outletId,
            @Param("menuItemId") UUID menuItemId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );

    @Query("""
            select i.outlet.id as outletId, i.menuItem.id as menuItemId,
                   (i.quantity - i.reservedQuantity) as available
            from InventoryItem i
            where i.outlet.id in :outletIds and i.menuItem.id in :menuItemIds
            """)
    List<StockAvailabilityView> findAvailability(
            @Param("outletIds") Collection<UUID> outletIds,
            @Param("menuItemIds") Collection<UUID> menuItemIds
    );
}
