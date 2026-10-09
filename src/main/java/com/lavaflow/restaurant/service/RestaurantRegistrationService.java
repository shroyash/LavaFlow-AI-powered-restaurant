package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.service.EmailVerificationService;
import com.lavaflow.common.enums.RestaurantDocumentType;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.dto.RegisterRestaurantRequest;
import com.lavaflow.restaurant.dto.RegisterRestaurantResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.mapper.RestaurantRegistrationMapper;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantRegistrationService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserProvisioner restaurantUserProvisioner;
    private final RestaurantDocumentService restaurantDocumentService;
    private final EmailVerificationService emailVerificationService;
    private final RestaurantRegistrationMapper restaurantRegistrationMapper;

    @Transactional
    public RegisterRestaurantResponse registerRestaurant(
            RegisterRestaurantRequest request,
            List<MultipartFile> documents,
            List<RestaurantDocumentType> documentTypes
    ) {
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                restaurantRegistrationMapper.toRestaurant(request)
        );

        User admin = restaurantUserProvisioner.provision(
                request.getAdminFullName(),
                request.getAdminEmail(),
                request.getAdminPhone(),
                request.getAdminPassword(),
                UserRole.RESTAURANT_OWNER,
                restaurant
        );

        restaurantDocumentService.storeDocuments(restaurant, documents, documentTypes);
        emailVerificationService.sendVerification(admin);

        return restaurantRegistrationMapper.toRegistrationResponse(restaurant, admin);
    }
}