package com.lavaflow.auth.service;

import com.lavaflow.auth.dto.AuthResponse;
import com.lavaflow.auth.dto.LoginRequest;
import com.lavaflow.auth.dto.RefreshTokenRequest;
import com.lavaflow.auth.dto.RegisterCustomerRequest;
import com.lavaflow.auth.entity.RefreshToken;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.exception.AccountInactiveException;
import com.lavaflow.auth.exception.EmailAlreadyExistsException;
import com.lavaflow.auth.mapper.AuthMapper;
import com.lavaflow.auth.repository.RefreshTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.auth.security.JwtProperties;
import com.lavaflow.auth.security.JwtService;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final TokenRevocationService tokenRevocationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final EmailVerificationService emailVerificationService;
    private final AccountAccessValidator accountAccessValidator;

    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = authMapper.toUser(request);
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizePhone(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setRestaurant(null);
        user.setActive(true);
        user.setEmailVerified(false);

        user = userRepository.saveAndFlush(user);

        emailVerificationService.sendVerification(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        accountAccessValidator.validate(user);

        if (!user.isActive()) {
            throw new AccountInactiveException(
                    "Your account has been deactivated. Please contact support."
            );
        }

        refreshTokenRepository.revokeAllByUser(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String tokenStr = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(tokenStr)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or expired refresh token.")
                );

        User user = refreshToken.getUser();

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(user);
            tokenRevocationService.revokeUserTokens(user.getId(), Instant.now());

            log.warn("Refresh token reuse detected for userId: {}. All sessions terminated.",
                    user.getId());

            throw new SecurityException(
                    "This refresh token has already been used. " +
                            "All active sessions have been terminated for your security."
            );
        }

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);

            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        if (!user.isActive()) {
            throw new AccountInactiveException("Your account has been deactivated.");
        }

        String newRefreshTokenStr = jwtService.generateRefreshTokenString();

        refreshToken.setRevoked(true);
        refreshToken.setReplacedByToken(newRefreshTokenStr);
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setToken(newRefreshTokenStr);
        newRefreshToken.setExpiryDate(
                LocalDateTime.now().plus(jwtProperties.getRefreshTokenExpiration())
        );
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getRole(), getRestaurantId(user)
        );

        return authMapper.toAuthResponse(
                user, accessToken, newRefreshTokenStr, jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void logout(String refreshTokenStr) {

        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);

                    User user = refreshToken.getUser();
                    tokenRevocationService.revokeUserTokens(user.getId(), Instant.now());

                    log.info("User {} logged out. Refresh token revoked and access tokens invalidated.",
                            user.getId());
                });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getRole(), getRestaurantId(user)
        );
        RefreshToken refreshToken = createRefreshToken(user);
        return authMapper.toAuthResponse(
                user, accessToken, refreshToken.getToken(), jwtService.getAccessTokenExpirationSeconds()
        );
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshTokenString());
        refreshToken.setExpiryDate(
                LocalDateTime.now().plus(jwtProperties.getRefreshTokenExpiration())
        );
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    private UUID getRestaurantId(User user) {
        return user.getRestaurant() != null ? user.getRestaurant().getId() : null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
