package com.lavaflow.auth.service;

import com.lavaflow.auth.dto.AuthResponse;
import com.lavaflow.auth.dto.LoginRequest;
import com.lavaflow.auth.dto.RefreshTokenRequest;
import com.lavaflow.auth.entity.RefreshToken;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.exception.AccountInactiveException;
import com.lavaflow.auth.repository.RefreshTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.auth.security.JwtProperties;
import com.lavaflow.auth.security.JwtService;
import com.lavaflow.auth.security.UserDetailsServiceImpl;
import com.lavaflow.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final TokenRevocationService tokenRevocationService;


    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        if (!user.isActive()) {
            throw new AccountInactiveException("Your account has been deactivated. Please contact support.");
        }

        refreshTokenRepository.revokeAllByUser(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getRole(),
                getRestaurantId(user)
        );

        RefreshToken refreshToken = createRefreshToken(user);

        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }


    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token."));

        User user = refreshToken.getUser();

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(user);
            tokenRevocationService.revokeUserTokens(user.getId(), Instant.now());
            log.warn("Refresh token reuse detected for userId: {}. All sessions terminated.", user.getId());
            throw new SecurityException(
                    "This refresh token has already been used. All active sessions have been terminated for your security.");
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
        newRefreshToken.setExpiryDate(LocalDateTime.now().plus(jwtProperties.getRefreshTokenExpiration()));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getRole(),
                getRestaurantId(user)
        );

        return buildAuthResponse(accessToken, newRefreshTokenStr, user);
    }


    public void logout(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            tokenRevocationService.revokeUserTokens(rt.getUser().getId(), Instant.now());
            log.info("User {} logged out. Refresh token revoked, access tokens invalidated.",
                    rt.getUser().getId());
        });
    }

    // Private helpers

    private RefreshToken createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(jwtService.generateRefreshTokenString());
        rt.setExpiryDate(LocalDateTime.now().plus(jwtProperties.getRefreshTokenExpiration()));
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    private UUID getRestaurantId(User user) {
        return user.getRestaurant() != null ? user.getRestaurant().getId() : null;
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshTokenStr, User user) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .restaurantId(getRestaurantId(user) != null ? getRestaurantId(user).toString() : null)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(userInfo)
                .build();
    }
}
