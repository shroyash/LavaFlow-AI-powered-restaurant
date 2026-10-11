package com.lavaflow.restaurant.security;

import com.lavaflow.auth.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentRestaurantResolver {

    public UUID resolve(UserPrincipal principal) {
        if (principal == null || principal.getUser().getRestaurant() == null) {
            throw new AccessDeniedException("You are not associated with a restaurant.");
        }
        return principal.getUser().getRestaurant().getId();
    }
}