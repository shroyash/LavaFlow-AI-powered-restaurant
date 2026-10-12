package com.lavaflow.auth.service;

import com.lavaflow.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService tokenRevocationService;

    @Transactional
    public void revokeAllSessions(UUID userId) {
        refreshTokenRepository.deleteAllByUserIdIn(List.of(userId));
        tokenRevocationService.revokeUserTokens(userId, Instant.now());
    }
}