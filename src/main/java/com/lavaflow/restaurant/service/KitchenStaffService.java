package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.dto.CreateKitchenStaffRequest;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.mapper.KitchenStaffMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenStaffService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserProvisioner restaurantUserProvisioner;
    private final KitchenStaffMapper kitchenStaffMapper;

    @Transactional
    public KitchenStaffResponse createKitchenStaff(UUID restaurantId, CreateKitchenStaffRequest request) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        User kitchenStaff = restaurantUserProvisioner.provision(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                UserRole.KITCHEN_STAFF,
                restaurant
        );

        return kitchenStaffMapper.toResponse(kitchenStaff);
    }
}