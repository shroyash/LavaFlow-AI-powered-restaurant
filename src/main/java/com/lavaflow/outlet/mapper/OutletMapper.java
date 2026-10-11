package com.lavaflow.outlet.mapper;

import com.lavaflow.outlet.dto.CreateOutletRequest;
import com.lavaflow.outlet.dto.OutletResponse;
import com.lavaflow.outlet.dto.UpdateOutletRequest;
import com.lavaflow.outlet.entity.Outlet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OutletMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    Outlet toEntity(CreateOutletRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(UpdateOutletRequest request, @MappingTarget Outlet outlet);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    OutletResponse toResponse(Outlet outlet);
}