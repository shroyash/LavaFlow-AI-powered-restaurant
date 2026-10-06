package com.lavaflow.auth.service;

import com.lavaflow.auth.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Redis-backed token revocation service.
 * REUSED from College Bridge UserTokenRevocationService:
 * - Same Redis key/value strategy (revoked-at epoch seconds).
 * - Same TTL (access token lifetime).
 * - Long userId → UUID userId.
 * - Key prefix: "lavaflow:token:revoked-at:{userId}"
 */
@Service
@RequiredArgsConstructor
public class RedisTokenRevocationService implements TokenRevocationService {

    private static final String KEY_PREFIX = "lavaflow:token:revoked-at:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void revokeUserTokens(UUID userId, Instant revokedAt) {
        String key = KEY_PREFIX + userId;
        Duration ttl = jwtProperties.getAccessTokenExpiration();
        redisTemplate.opsForValue().set(key, String.valueOf(revokedAt.getEpochSecond()), ttl);
    }

    @Override
    public Instant getRevokedAt(UUID userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (value == null) return null;
        return Instant.ofEpochSecond(Long.parseLong(value));
    }

    @Override
    public boolean isTokenRevoked(UUID userId, Instant issuedAt) {
        Instant revokedAt = getRevokedAt(userId);
        if (revokedAt == null) return false;
        return !issuedAt.isAfter(revokedAt);
    }
}
