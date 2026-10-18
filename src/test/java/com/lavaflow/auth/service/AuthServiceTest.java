package com.lavaflow.auth.service;

import com.lavaflow.auth.dto.AuthResponse;
import com.lavaflow.auth.dto.LoginRequest;
import com.lavaflow.auth.dto.RefreshTokenRequest;
import com.lavaflow.auth.entity.RefreshToken;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.exception.AccountInactiveException;
import com.lavaflow.auth.mapper.AuthMapper;
import com.lavaflow.auth.repository.RefreshTokenRepository;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.auth.security.JwtProperties;
import com.lavaflow.auth.security.JwtService;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenRevocationService tokenRevocationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthMapper authMapper;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AccountAccessValidator accountAccessValidator;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Restaurant testRestaurant;
    private UUID userId;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        testRestaurant = new Restaurant();
        testRestaurant.setId(restaurantId);

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("staff@lavaflow.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFullName("John Doe");
        testUser.setRole(UserRole.RESTAURANT_STAFF);
        testUser.setRestaurant(testRestaurant);
        testUser.setActive(true);
        testUser.setEmailVerified(true);

        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(7));
    }

    @Test
    @DisplayName("1. Login with valid credentials succeeds and issues tokens")
    void loginWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("staff@lavaflow.com");
        request.setPassword("correctPassword");

        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateAccessToken(eq(userId), eq(UserRole.RESTAURANT_STAFF), eq(restaurantId)))
                .thenReturn("access-token-123");
        when(jwtService.generateRefreshTokenString()).thenReturn("refresh-token-123");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        RefreshToken savedToken = new RefreshToken();
        savedToken.setToken("refresh-token-123");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();
        when(authMapper.toAuthResponse(eq(testUser), eq("access-token-123"), eq("refresh-token-123"), eq(900L)))
                .thenReturn(expectedResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-123");
        verify(refreshTokenRepository).revokeAllByUser(testUser);
        verify(accountAccessValidator).validate(testUser);
    }

    @Test
    @DisplayName("2. Login with invalid password throws BadCredentialsException")
    void loginWithInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("staff@lavaflow.com");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid email or password."));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("3. Login with deactivated account throws AccountInactiveException")
    void loginWithDeactivatedAccount() {
        testUser.setActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("staff@lavaflow.com");
        request.setPassword("password");

        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        doThrow(new AccountInactiveException("Your account has been deactivated."))
                .when(accountAccessValidator).validate(testUser);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountInactiveException.class);
    }

    @Test
    @DisplayName("8. Refresh token issued and rotated on refresh")
    void refreshTokenRotation() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");

        RefreshToken existingToken = new RefreshToken();
        existingToken.setUser(testUser);
        existingToken.setToken("old-refresh-token");
        existingToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        existingToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(existingToken));
        when(jwtService.generateRefreshTokenString()).thenReturn("new-refresh-token");
        when(jwtService.generateAccessToken(eq(userId), eq(UserRole.RESTAURANT_STAFF), eq(restaurantId)))
                .thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();
        when(authMapper.toAuthResponse(eq(testUser), eq("new-access-token"), eq("new-refresh-token"), eq(900L)))
                .thenReturn(expectedResponse);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(existingToken.isRevoked()).isTrue();
        assertThat(existingToken.getReplacedByToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("9. Refresh token reuse triggers invalidation of all sessions")
    void refreshTokenReuseDetected() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("already-used-refresh-token");

        RefreshToken reusedToken = new RefreshToken();
        reusedToken.setUser(testUser);
        reusedToken.setToken("already-used-refresh-token");
        reusedToken.setRevoked(true); // already used!

        when(refreshTokenRepository.findByToken("already-used-refresh-token")).thenReturn(Optional.of(reusedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("already been used");

        verify(refreshTokenRepository).revokeAllByUser(testUser);
        verify(tokenRevocationService).revokeUserTokens(eq(userId), any(Instant.class));
    }

    @Test
    @DisplayName("Logout revokes refresh token and invalidates access tokens via Redis")
    void logoutRevokesTokens() {
        RefreshToken token = new RefreshToken();
        token.setUser(testUser);
        token.setToken("token-to-logout");
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-logout")).thenReturn(Optional.of(token));

        authService.logout("token-to-logout");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
        verify(tokenRevocationService).revokeUserTokens(eq(userId), any(Instant.class));
    }
}
