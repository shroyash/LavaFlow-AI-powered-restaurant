package com.lavaflow.auth.security;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.service.TokenRevocationService;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.entity.Restaurant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private TokenRevocationService tokenRevocationService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private UUID userId;
    private UUID restaurantId;
    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("staff@lavaflow.com");
        testUser.setRole(UserRole.RESTAURANT_STAFF);
        testUser.setRestaurant(restaurant);
        testUser.setActive(true);

        testPrincipal = new UserPrincipal(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid JWT sets SecurityContext with principal and authorities")
    void validJwtSetsSecurityContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt-token");
        when(jwtService.parseClaims("valid-jwt-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("RESTAURANT_STAFF");
        when(claims.get("restaurantId", String.class)).thenReturn(restaurantId.toString());
        when(claims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() - 5000));
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60000));

        when(userDetailsService.loadByUserId(userId)).thenReturn(testPrincipal);
        when(tokenRevocationService.isTokenRevoked(eq(userId), any(Instant.class))).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(testPrincipal);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("7. Expired JWT leaves SecurityContext empty")
    void expiredJwtLeavesSecurityContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-jwt-token");
        when(jwtService.parseClaims("expired-jwt-token"))
                .thenThrow(new ExpiredJwtException(null, claims, "Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid/tampered JWT leaves SecurityContext empty")
    void invalidJwtLeavesSecurityContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-jwt-token");
        when(jwtService.parseClaims("invalid-jwt-token"))
                .thenThrow(new JwtException("Invalid signature"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("17. Revoked token rejected on subsequent requests")
    void revokedTokenLeavesSecurityContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked-jwt-token");
        when(jwtService.parseClaims("revoked-jwt-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("RESTAURANT_STAFF");
        when(claims.get("restaurantId", String.class)).thenReturn(restaurantId.toString());
        when(claims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() - 5000));
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60000));

        when(userDetailsService.loadByUserId(userId)).thenReturn(testPrincipal);
        when(tokenRevocationService.isTokenRevoked(eq(userId), any(Instant.class))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("16. Token with mismatched restaurantId is rejected (tenant isolation)")
    void mismatchedRestaurantIdLeavesSecurityContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer foreign-jwt-token");
        when(jwtService.parseClaims("foreign-jwt-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("RESTAURANT_STAFF");
        when(claims.get("restaurantId", String.class)).thenReturn(UUID.randomUUID().toString()); // Different restaurant!
        when(claims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() - 5000));
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60000));

        when(userDetailsService.loadByUserId(userId)).thenReturn(testPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
