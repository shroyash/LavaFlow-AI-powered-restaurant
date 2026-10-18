package com.lavaflow.kitchen.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.order.entity.Order;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "kitchen_orders",
        indexes = {
                @Index(name = "idx_kitchen_orders_restaurant_status", columnList = "restaurant_id,status"),
                @Index(name = "idx_kitchen_orders_status_created", columnList = "status,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class KitchenOrder extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private KitchenOrderStatus status = KitchenOrderStatus.PENDING;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "preparing_at")
    private LocalDateTime preparingAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
