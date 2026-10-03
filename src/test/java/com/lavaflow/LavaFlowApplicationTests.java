package com.lavaflow;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.*;
import com.lavaflow.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LavaFlowApplicationTests {

    @Test
    @DisplayName("Should successfully instantiate and verify entity relationships and enums")
    void testEntityInstantiation() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("LavaFlow Grill");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@lavaflow.ai");
        user.setFullName("John Doe");
        user.setRole(UserRole.RESTAURANT_OWNER);
        user.setRestaurant(restaurant);
        user.setActive(true);

        assertEquals("LavaFlow Grill", restaurant.getName());
        assertEquals(RestaurantStatus.ACTIVE, restaurant.getStatus());
        assertEquals("owner@lavaflow.ai", user.getEmail());
        assertEquals(UserRole.RESTAURANT_OWNER, user.getRole());
        assertEquals(restaurant, user.getRestaurant());
    }
}
