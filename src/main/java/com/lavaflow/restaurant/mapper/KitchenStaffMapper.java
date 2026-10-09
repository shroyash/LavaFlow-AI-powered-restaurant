package com.lavaflow.restaurant.mapper;

import com.lavaflow.auth.entity.User;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KitchenStaffMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    KitchenStaffResponse toResponse(User user);
}