package com.lavaflow.auth.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for access-token revocation.
 * REUSED from College Bridge TokenRevocationService:
 * - Same strategy: store revoked-at timestamp per user in Redis.
 * - Any access token with issuedAt <= revokedAt is considered revoked.
 * - Change: Long userId → UUID userId (LavaFlow uses UUID primary keys).
 */
public interface TokenRevocationService {

    void revokeUserTokens(UUID userId, Instant revokedAt);

    Instant getRevokedAt(UUID userId);

    boolean isTokenRevoked(UUID userId, Instant issuedAt);
}
