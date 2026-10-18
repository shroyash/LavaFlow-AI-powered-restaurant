package com.lavaflow.customer.mapper;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.mapper.DateTimeMapper;
import com.lavaflow.customer.dto.CustomerProfileResponse;
import com.lavaflow.customer.entity.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CustomerAddressMapper.class, DateTimeMapper.class})
public interface CustomerProfileMapper {

    @Mapping(target = "userId", source = "customer.id")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "emailVerified", source = "customer.emailVerified")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "phone", source = "customer.phone")
    @Mapping(target = "createdAt", source = "customer.createdAt")
    @Mapping(target = "defaultAddress", source = "defaultAddress")
    CustomerProfileResponse toResponse(User customer, CustomerAddress defaultAddress);
}
