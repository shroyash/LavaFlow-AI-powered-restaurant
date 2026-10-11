package com.lavaflow.restaurant.mapper;

import com.lavaflow.restaurant.dto.CreateRestaurantRequest;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.dto.UpdateRestaurantRequest;
import com.lavaflow.restaurant.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    Restaurant toEntity(CreateRestaurantRequest request);

    void updateEntity(UpdateRestaurantRequest request, @MappingTarget Restaurant restaurant);

    RestaurantResponse toResponse(Restaurant restaurant);

    default LocalDateTime map(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}