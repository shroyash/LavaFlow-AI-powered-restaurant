package com.lavaflow.menu.mapper;

import com.lavaflow.menu.dto.CreateMenuCategoryRequest;
import com.lavaflow.menu.dto.MenuCategoryResponse;
import com.lavaflow.menu.dto.UpdateMenuCategoryRequest;
import com.lavaflow.menu.entity.MenuCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    MenuCategory toEntity(CreateMenuCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntity(UpdateMenuCategoryRequest request, @MappingTarget MenuCategory category);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    MenuCategoryResponse toResponse(MenuCategory category);
}