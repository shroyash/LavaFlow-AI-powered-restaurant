package com.lavaflow.auth.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.exception.AccountInactiveException;
import com.lavaflow.auth.exception.EmailNotVerifiedException;
import com.lavaflow.auth.exception.RestaurantNotActiveException;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.entity.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class AccountAccessValidator {

    public void validate(User user) {
        if (!user.isActive()) {
            throw new AccountInactiveException("Your account has been deactivated. Please contact support.");
        }

        if (user.getRole() == UserRole.RESTAURANT_OWNER && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Please verify your email address using the link we sent you before logging in.");
        }

        Restaurant restaurant = user.getRestaurant();
        if (restaurant == null) {
            return;
        }

        switch (restaurant.getStatus()) {
            case ACTIVE -> {
            }
            case PENDING_APPROVAL -> throw new RestaurantNotActiveException(
                    "Your restaurant is awaiting verification. You can log in once it has been approved.");
            case REJECTED -> throw new RestaurantNotActiveException(
                    "Your restaurant registration was rejected. Please contact support.");
            case SUSPENDED -> throw new RestaurantNotActiveException(
                    "Your restaurant has been suspended. Please contact support.");
            case INACTIVE -> throw new RestaurantNotActiveException(
                    "Your restaurant is currently inactive. Please contact support.");
        }
    }
}