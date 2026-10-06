package com.lavaflow.common.security;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for accessing the currently authenticated user from the SecurityContext.
 *
 * Usage in controllers/services:
 *   UserPrincipal me = SecurityUtils.requireCurrentUser();
 *   UUID myRestaurantId = SecurityUtils.requireCurrentUser().getRestaurantId();
 *
 * IMPORTANT: These utilities derive context from the SecurityContext — the authenticated
 * user's data from the verified JWT. Never use request parameters for authorization context.
 *
 * Design decision vs. College Bridge:
 * CB scattered direct SecurityContextHolder.getContext() calls across services.
 * LavaFlow centralizes this in SecurityUtils for clean, testable access.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the current UserPrincipal, or empty if not authenticated.
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Returns the current UserPrincipal or throws IllegalStateException.
     * Use in protected endpoints where authentication is guaranteed.
     */
    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in SecurityContext"));
    }

    /**
     * Returns the current user's UUID.
     */
    public static UUID getCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    /**
     * Returns the current user's role.
     */
    public static UserRole getCurrentUserRole() {
        return requireCurrentUser().getRole();
    }

    /**
     * Returns the current user's restaurantId, or null for SUPER_ADMIN / unscoped CUSTOMER.
     *
     * CRITICAL: Use this (not request params) for all restaurant-scoped authorization.
     */
    public static UUID getCurrentRestaurantId() {
        return requireCurrentUser().getRestaurantId();
    }

    /**
     * Returns true if the current user is a SUPER_ADMIN.
     */
    public static boolean isSuperAdmin() {
        return getCurrentUser()
                .map(p -> p.getRole() == UserRole.SUPER_ADMIN)
                .orElse(false);
    }

    /**
     * Returns true if the current user belongs to the given restaurant.
     * SUPER_ADMIN always returns true (cross-restaurant access).
     */
    public static boolean belongsToRestaurant(UUID restaurantId) {
        return getCurrentUser().map(principal -> {
            if (principal.getRole() == UserRole.SUPER_ADMIN) return true;
            return restaurantId.equals(principal.getRestaurantId());
        }).orElse(false);
    }
}
