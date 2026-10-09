package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.RestaurantVerificationResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.entity.RestaurantDocument;
import com.lavaflow.restaurant.event.RestaurantReviewedEvent;
import com.lavaflow.restaurant.exception.InvalidRestaurantStatusException;
import com.lavaflow.restaurant.mapper.RestaurantMapper;
import com.lavaflow.restaurant.mapper.RestaurantVerificationMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantVerificationService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantDocumentService restaurantDocumentService;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantVerificationMapper restaurantVerificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public RestaurantVerificationResponse getVerification(UUID restaurantId) {
        Restaurant restaurant = findOrThrow(restaurantId);
        User admin = findAdminOrThrow(restaurantId);
        List<RestaurantDocument> documents = restaurantDocumentService.getDocuments(restaurantId);

        return restaurantVerificationMapper.toResponse(restaurant, admin, documents);
    }

    @Transactional
    public RestaurantResponse approve(UUID restaurantId, UUID reviewerId) {
        Restaurant restaurant = findPendingOrThrow(restaurantId);
        User admin = findAdminOrThrow(restaurantId);

        if (!admin.isEmailVerified()) {
            throw new InvalidRestaurantStatusException(
                    "The restaurant admin has not verified their email address yet."
            );
        }

        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setRejectionReason(null);
        restaurant.setReviewedAt(LocalDateTime.now());
        restaurant.setReviewedBy(reviewerId);

        Restaurant saved = restaurantRepository.save(restaurant);

        eventPublisher.publishEvent(new RestaurantReviewedEvent(
                admin.getEmail(), admin.getFullName(), saved.getName(), true, null
        ));

        return restaurantMapper.toResponse(saved);
    }

    @Transactional
    public RestaurantResponse reject(UUID restaurantId, UUID reviewerId, String reason) {
        Restaurant restaurant = findPendingOrThrow(restaurantId);
        User admin = findAdminOrThrow(restaurantId);

        restaurant.setStatus(RestaurantStatus.REJECTED);
        restaurant.setRejectionReason(reason.trim());
        restaurant.setReviewedAt(LocalDateTime.now());
        restaurant.setReviewedBy(reviewerId);

        Restaurant saved = restaurantRepository.save(restaurant);

        if (admin.isEmailVerified()) {
            eventPublisher.publishEvent(new RestaurantReviewedEvent(
                    admin.getEmail(), admin.getFullName(), saved.getName(), false, saved.getRejectionReason()
            ));
        }

        return restaurantMapper.toResponse(saved);
    }

    private Restaurant findOrThrow(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));
    }

    private Restaurant findPendingOrThrow(UUID restaurantId) {
        Restaurant restaurant = findOrThrow(restaurantId);
        if (restaurant.getStatus() != RestaurantStatus.PENDING_APPROVAL) {
            throw new InvalidRestaurantStatusException("Only restaurants awaiting approval can be reviewed.");
        }
        return restaurant;
    }

    private User findAdminOrThrow(UUID restaurantId) {
        return userRepository.findFirstByRestaurantIdAndRole(restaurantId, UserRole.RESTAURANT_OWNER)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant admin not found."));
    }
}