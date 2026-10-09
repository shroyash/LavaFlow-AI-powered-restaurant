package com.lavaflow.auth.mapper;

import com.lavaflow.auth.dto.AuthResponse;
import com.lavaflow.auth.dto.RegisterCustomerRequest;
import com.lavaflow.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    AuthResponse.UserInfo toUserInfo(User user);

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "user", source = "user")
    AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, long expiresIn);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "CUSTOMER")
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "active", constant = "true")
    User toUser(RegisterCustomerRequest request);
}
