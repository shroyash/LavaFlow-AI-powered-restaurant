package com.lavaflow.restaurant.mapper;

import com.lavaflow.auth.entity.User;
import com.lavaflow.restaurant.dto.RestaurantDocumentResponse;
import com.lavaflow.restaurant.dto.RestaurantVerificationResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.entity.RestaurantDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(componentModel = "spring", uses = RestaurantMapper.class)
public interface RestaurantVerificationMapper {

    @Mapping(target = "restaurant", source = "restaurant")
    @Mapping(target = "adminFullName", source = "admin.fullName")
    @Mapping(target = "adminEmail", source = "admin.email")
    @Mapping(target = "adminPhone", source = "admin.phone")
    @Mapping(target = "adminEmailVerified", source = "admin.emailVerified")
    @Mapping(target = "documents", source = "documents")
    @Mapping(target = "rejectionReason", source = "restaurant.rejectionReason")
    @Mapping(target = "reviewedAt", source = "restaurant.reviewedAt")
    RestaurantVerificationResponse toResponse(Restaurant restaurant, User admin, List<RestaurantDocument> documents);

    @Mapping(target = "uploadedAt", source = "createdAt")
    RestaurantDocumentResponse toDocumentResponse(RestaurantDocument document);
}