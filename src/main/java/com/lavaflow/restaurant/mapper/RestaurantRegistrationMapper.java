package com.lavaflow.restaurant.mapper;

import com.lavaflow.auth.entity.User;
import com.lavaflow.restaurant.dto.RegisterRestaurantRequest;
import com.lavaflow.restaurant.dto.RegisterRestaurantResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantRegistrationMapper {

    @Mapping(target = "name", expression = "java(request.getRestaurantName().trim())")
    @Mapping(target = "description", source = "restaurantDescription")
    @Mapping(target = "phone", source = "restaurantPhone")
    @Mapping(target = "email", source = "restaurantEmail")
    @Mapping(target = "status", constant = "INACTIVE")
    Restaurant toRestaurant(RegisterRestaurantRequest request);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "restaurantStatus", source = "restaurant.status")
    @Mapping(target = "adminUserId", source = "admin.id")
    @Mapping(target = "adminEmail", source = "admin.email")
    RegisterRestaurantResponse toRegistrationResponse(Restaurant restaurant, User admin);
}