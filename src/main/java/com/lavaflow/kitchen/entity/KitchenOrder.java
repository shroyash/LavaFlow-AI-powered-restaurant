package com.lavaflow.kitchen.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.order.entity.Order;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "kitchen_orders")
@Getter
@Setter
@NoArgsConstructor
public class KitchenOrder extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private KitchenOrderStatus status = KitchenOrderStatus.PENDING;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "preparing_at")
    private Instant preparingAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
