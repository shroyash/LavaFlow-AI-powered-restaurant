package com.lavaflow.customer.mapper;

import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.dto.UpdateCustomerAddressRequest;
import com.lavaflow.customer.entity.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerAddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "defaultAddress", ignore = true)
    CustomerAddress toEntity(CreateCustomerAddressRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "defaultAddress", ignore = true)
    void updateEntity(UpdateCustomerAddressRequest request, @MappingTarget CustomerAddress address);

    CustomerAddressResponse toResponse(CustomerAddress address);
}