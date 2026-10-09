package com.lavaflow.restaurant.security;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("restaurantAccessPolicy")
public class RestaurantAccessPolicy {

    public boolean belongsToRestaurant(Authentication authentication, UUID restaurantId) {
        User user = currentUser(authentication);
        return user != null
                && user.getRestaurant() != null
                && restaurantId.equals(user.getRestaurant().getId());
    }

    public boolean isAdminOfRestaurant(Authentication authentication, UUID restaurantId) {
        User user = currentUser(authentication);
        return user != null
                && user.getRole() == UserRole.RESTAURANT_OWNER
                && belongsToRestaurant(authentication, restaurantId);
    }

    private User currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return null;
    }
}