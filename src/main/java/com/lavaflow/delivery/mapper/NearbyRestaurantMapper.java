package com.lavaflow.delivery.mapper;

import com.lavaflow.delivery.dto.NearbyRestaurantResponse;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.restaurant.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface NearbyRestaurantMapper {

    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "description", source = "restaurant.description")
    @Mapping(target = "logoUrl", source = "restaurant.logoUrl")
    @Mapping(target = "outletId", source = "outlet.id")
    @Mapping(target = "outletName", source = "outlet.name")
    @Mapping(target = "outletAddress", source = "outlet.address")
    @Mapping(target = "busy", expression = "java(outlet.getStatus() == com.lavaflow.common.enums.OutletStatus.BUSY)")
    @Mapping(target = "distanceKm", source = "distanceKm")
    @Mapping(target = "estimatedDeliveryMinutes", source = "estimatedDeliveryMinutes")
    NearbyRestaurantResponse toResponse(
            Restaurant restaurant,
            Outlet outlet,
            BigDecimal distanceKm,
            int estimatedDeliveryMinutes
    );
}