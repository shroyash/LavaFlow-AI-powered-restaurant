package com.lavaflow.restaurant.service;

import com.lavaflow.common.storage.PublicImageStorage;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.mapper.RestaurantMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantLogoService {

    private final RestaurantRepository restaurantRepository;
    private final PublicImageStorage publicImageStorage;
    private final RestaurantMapper restaurantMapper;

    @Transactional
    public RestaurantResponse uploadLogo(UUID restaurantId, MultipartFile file) {
        Restaurant restaurant = findRestaurant(restaurantId);
        String previousLogo = restaurant.getLogoUrl();

        restaurant.setLogoUrl(publicImageStorage.store("logos/" + restaurantId, file));
        Restaurant saved = restaurantRepository.save(restaurant);

        publicImageStorage.deleteOnCommit(previousLogo);
        return restaurantMapper.toResponse(saved);
    }

    @Transactional
    public RestaurantResponse removeLogo(UUID restaurantId) {
        Restaurant restaurant = findRestaurant(restaurantId);
        String previousLogo = restaurant.getLogoUrl();

        restaurant.setLogoUrl(null);
        Restaurant saved = restaurantRepository.save(restaurant);

        publicImageStorage.deleteOnCommit(previousLogo);
        return restaurantMapper.toResponse(saved);
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));
    }
}