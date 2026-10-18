package com.lavaflow.kitchen.repository;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.kitchen.entity.KitchenOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KitchenOrderRepository
        extends JpaRepository<KitchenOrder, UUID>, JpaSpecificationExecutor<KitchenOrder> {

    @Override
    @EntityGraph(attributePaths = {"order", "outlet"})
    Page<KitchenOrder> findAll(Specification<KitchenOrder> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "outlet"})
    Optional<KitchenOrder> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    Optional<KitchenOrder> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    List<KitchenOrder> findByStatusAndCreatedAtBefore(KitchenOrderStatus status, LocalDateTime threshold);
}
