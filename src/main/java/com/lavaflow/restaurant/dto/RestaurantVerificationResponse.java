package com.lavaflow.restaurant.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RestaurantVerificationResponse {

    private RestaurantResponse restaurant;
    private String adminFullName;
    private String adminEmail;
    private String adminPhone;
    private boolean adminEmailVerified;
    private List<RestaurantDocumentResponse> documents;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
}