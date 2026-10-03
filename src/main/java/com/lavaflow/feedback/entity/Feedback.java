package com.lavaflow.feedback.entity;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.order.entity.Order;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class Feedback extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "sentiment", length = 50)
    private String sentiment;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "requires_escalation", nullable = false)
    private boolean requiresEscalation = false;
}
