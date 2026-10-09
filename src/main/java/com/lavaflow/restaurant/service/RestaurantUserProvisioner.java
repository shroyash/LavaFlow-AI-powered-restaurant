package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.exception.EmailAlreadyExistsException;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RestaurantUserProvisioner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User provision(
            String fullName,
            String email,
            String phone,
            String rawPassword,
            UserRole role,
            Restaurant restaurant
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(fullName.trim());
        user.setPhone(normalizePhone(phone));
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setRestaurant(restaurant);
        user.setActive(true);

        return userRepository.saveAndFlush(user);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}