package com.lavaflow.auth.service;

import com.lavaflow.auth.entity.EmailVerificationToken;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.event.EmailVerificationRequestedEvent;
import com.lavaflow.auth.exception.InvalidEmailVerificationTokenException;
import com.lavaflow.auth.repository.EmailVerificationTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int TOKEN_BYTES = 32;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${lavaflow.email-verification.expiry-hours:24}")
    private long expiryHours;

    @Transactional
    public void sendVerification(User user) {
        tokenRepository.deleteByUser(user);

        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusHours(expiryHours));
        tokenRepository.save(token);

        eventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(user.getEmail(), user.getFullName(), rawToken)
        );
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken.trim()))
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidEmailVerificationTokenException();
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUser(user);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .filter(user -> !user.isEmailVerified())
                .filter(user -> !issuedRecently(user))
                .ifPresent(this::sendVerification);
    }

    private boolean issuedRecently(User user) {
        return tokenRepository.findTopByUserOrderByIssuedAtDesc(user)
                .map(token -> token.getIssuedAt().isAfter(LocalDateTime.now().minus(RESEND_COOLDOWN)))
                .orElse(false);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}