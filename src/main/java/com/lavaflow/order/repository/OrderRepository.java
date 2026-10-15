package com.lavaflow.order.repository;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = {"restaurant", "outlet"})
    Page<Order> findAll(Specification<Order> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "outlet", "customer", "items"})
    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = {"restaurant", "outlet", "customer", "items"})
    Optional<Order> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(UUID id);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);

    @EntityGraph(attributePaths = {"restaurant", "outlet", "customer", "items", "items.menuItem"})
    Optional<Order> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}