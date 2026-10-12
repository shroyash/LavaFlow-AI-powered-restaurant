package com.lavaflow.delivery.mapper;

import com.lavaflow.delivery.dto.NearestOutletResponse;
import com.lavaflow.outlet.entity.Outlet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface NearestOutletMapper {

    @Mapping(target = "outletId", source = "outlet.id")
    @Mapping(target = "outletName", source = "outlet.name")
    @Mapping(target = "address", source = "outlet.address")
    @Mapping(target = "phone", source = "outlet.phone")
    @Mapping(target = "latitude", source = "outlet.latitude")
    @Mapping(target = "longitude", source = "outlet.longitude")
    @Mapping(target = "outletStatus", source = "outlet.status")
    @Mapping(target = "busy", expression = "java(outlet.getStatus() == com.lavaflow.common.enums.OutletStatus.BUSY)")
    @Mapping(target = "distanceKm", source = "distanceKm")
    @Mapping(target = "estimatedDeliveryMinutes", source = "estimatedDeliveryMinutes")
    NearestOutletResponse toResponse(Outlet outlet, BigDecimal distanceKm, int estimatedDeliveryMinutes);
}