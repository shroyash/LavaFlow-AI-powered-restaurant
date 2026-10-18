package com.lavaflow.auth.mapper;

import com.lavaflow.auth.dto.UpdateProfileRequest;
import com.lavaflow.auth.dto.UserProfileResponse;
import com.lavaflow.auth.entity.User;
import com.lavaflow.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = DateTimeMapper.class)
public interface UserProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    void updateEntity(UpdateProfileRequest request, @MappingTarget User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    UserProfileResponse toResponse(User user);
}
