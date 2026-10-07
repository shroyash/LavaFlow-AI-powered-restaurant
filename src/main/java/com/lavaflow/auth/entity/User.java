package com.lavaflow.auth.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Core user entity for LavaFlow. A single user row represents every role on the platform.
 * restaurant is NULL for SUPER_ADMIN and optionally NULL for CUSTOMER.
 * is_active replaces the status enum: a simple boolean keeps the model clean.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role = UserRole.CUSTOMER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
