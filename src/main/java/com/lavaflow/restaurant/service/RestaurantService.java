package com.lavaflow.restaurant.service;

import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.restaurant.dto.CreateRestaurantRequest;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.UpdateRestaurantRequest;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.mapper.RestaurantMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Transactional
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setName(restaurant.getName().trim());
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getRestaurants(RestaurantStatus status, Pageable pageable) {
        Page<Restaurant> restaurants = status == null
                ? restaurantRepository.findAll(pageable)
                : restaurantRepository.findByStatus(status, pageable);
        return restaurants.map(restaurantMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID id) {
        return restaurantMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public RestaurantResponse updateRestaurant(UUID id, UpdateRestaurantRequest request) {
        Restaurant restaurant = findOrThrow(id);
        restaurantMapper.updateEntity(request, restaurant);
        restaurant.setName(restaurant.getName().trim());
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    private Restaurant findOrThrow(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));
    }
}