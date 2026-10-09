package com.lavaflow.restaurant.controller;

import com.lavaflow.common.enums.RestaurantDocumentType;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.restaurant.dto.RegisterRestaurantRequest;
import com.lavaflow.restaurant.dto.RegisterRestaurantResponse;
import com.lavaflow.restaurant.service.RestaurantRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurant Registration", description = "Public restaurant onboarding")
public class RestaurantRegistrationController {

    private final RestaurantRegistrationService restaurantRegistrationService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a restaurant with verification documents; login is enabled after approval")
    public ResponseEntity<ApiResponse<RegisterRestaurantResponse>> register(
            @Valid @ModelAttribute RegisterRestaurantRequest request,
            @RequestParam("documents") List<MultipartFile> documents,
            @RequestParam("documentTypes") List<RestaurantDocumentType> documentTypes
    ) {
        RegisterRestaurantResponse response =
                restaurantRegistrationService.registerRestaurant(request, documents, documentTypes);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Registration submitted. Please verify your email using the link we sent you. You can log in once our team approves your restaurant.",
                response
        ));
    }
}