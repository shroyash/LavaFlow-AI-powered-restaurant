package com.lavaflow.auth.service;

import java.time.Instant;
import java.util.UUID;


public interface TokenRevocationService {

    void revokeUserTokens(UUID userId, Instant revokedAt);

    Instant getRevokedAt(UUID userId);

    boolean isTokenRevoked(UUID userId, Instant issuedAt);
}
