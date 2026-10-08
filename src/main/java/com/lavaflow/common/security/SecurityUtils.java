package com.lavaflow.common.security;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;


public final class SecurityUtils {

    private SecurityUtils() {}


    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }


    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in SecurityContext"));
    }


    public static UUID getCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    public static UserRole getCurrentUserRole() {
        return requireCurrentUser().getRole();
    }


    public static UUID getCurrentRestaurantId() {
        return requireCurrentUser().getRestaurantId();
    }


    public static boolean isSuperAdmin() {
        return getCurrentUser()
                .map(p -> p.getRole() == UserRole.SUPER_ADMIN)
                .orElse(false);
    }

    public static boolean belongsToRestaurant(UUID restaurantId) {
        return getCurrentUser().map(principal -> {
            if (principal.getRole() == UserRole.SUPER_ADMIN) return true;
            return restaurantId.equals(principal.getRestaurantId());
        }).orElse(false);
    }
}
