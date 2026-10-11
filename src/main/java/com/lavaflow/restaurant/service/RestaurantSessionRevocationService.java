package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.RefreshTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.auth.service.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantSessionRevocationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService tokenRevocationService;

    @Transactional
    public void revokeAllSessions(UUID restaurantId) {
        List<UUID> userIds = userRepository.findByRestaurantId(restaurantId).stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return;
        }

        refreshTokenRepository.deleteAllByUserIdIn(userIds);

        Instant revokedAt = Instant.now();
        userIds.forEach(userId -> tokenRevocationService.revokeUserTokens(userId, revokedAt));

        log.info("Revoked sessions of {} users for restaurant {}", userIds.size(), restaurantId);
    }
}