package com.lavaflow.delivery.entity;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.RiderStatus;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "riders")
@Getter
@Setter
@NoArgsConstructor
public class Rider extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RiderStatus status = RiderStatus.AVAILABLE;
}
