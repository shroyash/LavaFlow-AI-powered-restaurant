package com.lavaflow.auth.repository;

import com.lavaflow.auth.entity.EmailVerificationToken;
import com.lavaflow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findTopByUserOrderByIssuedAtDesc(User user);

    void deleteByUser(User user);
}