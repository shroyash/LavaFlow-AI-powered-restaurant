package com.lavaflow.menu.mapper;

import com.lavaflow.menu.dto.CreateMenuItemRequest;
import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.dto.UpdateMenuItemRequest;
import com.lavaflow.menu.entity.MenuItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    MenuItem toEntity(CreateMenuItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(UpdateMenuItemRequest request, @MappingTarget MenuItem item);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    MenuItemResponse toResponse(MenuItem item);
}