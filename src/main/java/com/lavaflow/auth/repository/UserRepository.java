package com.lavaflow.auth.repository;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findFirstByRestaurantIdAndRole(UUID restaurantId, UserRole role);

    List<User> findByRestaurantId(UUID restaurantId);
    boolean existsByRole(UserRole role);
}
