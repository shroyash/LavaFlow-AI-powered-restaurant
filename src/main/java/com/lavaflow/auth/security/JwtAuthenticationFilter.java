package com.lavaflow.auth.security;

import com.lavaflow.auth.service.TokenRevocationService;
import com.lavaflow.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;


@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            TokenRevocationService tokenRevocationService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseClaims(jwt);

            String userIdStr       = claims.getSubject();
            String roleStr         = claims.get("role", String.class);
            String restaurantIdStr = claims.get("restaurantId", String.class);
            Date   issuedAtDate    = claims.getIssuedAt();
            Date   expirationDate  = claims.getExpiration();

            if (userIdStr == null || roleStr == null || issuedAtDate == null || expirationDate == null) {
                log.debug("JWT missing required claims");
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId;
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("JWT sub is not a valid UUID: {}", userIdStr);
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserPrincipal principal = userDetailsService.loadByUserId(userId);

                if (!principal.getRole().name().equals(roleStr)) {
                    log.warn("JWT role mismatch. JWT={}, DB={}", roleStr, principal.getRole());
                    filterChain.doFilter(request, response);
                    return;
                }

                UUID jwtRestaurantId = parseUuidSilently(restaurantIdStr);
                UUID dbRestaurantId  = principal.getRestaurantId();

                if (principal.getRole() != UserRole.SUPER_ADMIN
                        && !Objects.equals(jwtRestaurantId, dbRestaurantId)) {
                    log.warn("JWT restaurantId mismatch. JWT={}, DB={}, userId={}", jwtRestaurantId, dbRestaurantId, userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!principal.isEnabled()) {
                    log.warn("Rejected JWT for inactive userId={}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                Instant issuedAt = issuedAtDate.toInstant();
                if (tokenRevocationService.isTokenRevoked(userId, issuedAt)) {
                    log.warn("Rejected revoked token for userId={}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Security context error: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        }
    }

    private UUID parseUuidSilently(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
