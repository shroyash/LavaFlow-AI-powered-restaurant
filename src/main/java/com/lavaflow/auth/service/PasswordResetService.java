package com.lavaflow.auth.service;

import com.lavaflow.auth.entity.PasswordResetToken;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.event.PasswordResetRequestedEvent;
import com.lavaflow.auth.repository.PasswordResetTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final String INVALID_TOKEN_MESSAGE = "This password reset link is invalid or has expired.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenGenerator tokenGenerator;
    private final UserSessionRevocationService userSessionRevocationService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${lavaflow.password-reset.expiry-minutes:30}")
    private long expiryMinutes;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .filter(user -> !requestedRecently(user))
                .ifPresent(this::issueToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenGenerator.hash(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        User user = token.getUser();
        if (!user.isActive()) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUser(user);

        userSessionRevocationService.revokeAllSessions(user.getId());
    }

    private void issueToken(User user) {
        tokenRepository.deleteByUser(user);

        String rawToken = tokenGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenGenerator.hash(rawToken));
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusMinutes(expiryMinutes));
        tokenRepository.save(token);

        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(user.getEmail(), user.getFullName(), rawToken)
        );
    }

    private boolean requestedRecently(User user) {
        return tokenRepository.findTopByUserOrderByIssuedAtDesc(user)
                .map(token -> token.getIssuedAt().isAfter(LocalDateTime.now().minus(REQUEST_COOLDOWN)))
                .orElse(false);
    }
}