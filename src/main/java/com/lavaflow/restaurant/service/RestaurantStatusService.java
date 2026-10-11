package com.lavaflow.restaurant.service;

import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.event.RestaurantAccessRevokedEvent;
import com.lavaflow.restaurant.exception.InvalidRestaurantStatusException;
import com.lavaflow.restaurant.mapper.RestaurantMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final Set<RestaurantStatus> ACCESS_REVOKING_STATUSES =
            EnumSet.of(RestaurantStatus.INACTIVE, RestaurantStatus.SUSPENDED);

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RestaurantResponse changeStatus(UUID id, RestaurantStatus status) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        RestaurantStatus previousStatus = restaurant.getStatus();

        if (REVIEW_STATUSES.contains(previousStatus) || REVIEW_STATUSES.contains(status)) {
            throw new InvalidRestaurantStatusException(
                    "Use the verification endpoints to approve or reject a restaurant."
            );
        }

        restaurant.setStatus(status);
        Restaurant saved = restaurantRepository.save(restaurant);

        if (previousStatus != status && ACCESS_REVOKING_STATUSES.contains(status)) {
            eventPublisher.publishEvent(new RestaurantAccessRevokedEvent(saved.getId()));
        }

        return restaurantMapper.toResponse(saved);
    }
}