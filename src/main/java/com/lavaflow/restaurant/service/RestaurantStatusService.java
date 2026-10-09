package com.lavaflow.restaurant.service;

import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.exception.InvalidRestaurantStatusException;
import com.lavaflow.restaurant.mapper.RestaurantMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantStatusService {

    private static final Set<RestaurantStatus> REVIEW_STATUSES =
            EnumSet.of(RestaurantStatus.PENDING_APPROVAL, RestaurantStatus.REJECTED);

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Transactional
    public RestaurantResponse changeStatus(UUID id, RestaurantStatus status) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        if (REVIEW_STATUSES.contains(restaurant.getStatus()) || REVIEW_STATUSES.contains(status)) {
            throw new InvalidRestaurantStatusException(
                    "Use the verification endpoints to approve or reject a restaurant."
            );
        }

        restaurant.setStatus(status);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }
}