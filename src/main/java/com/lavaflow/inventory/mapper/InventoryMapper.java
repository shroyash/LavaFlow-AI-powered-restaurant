package com.lavaflow.inventory.mapper;

import com.lavaflow.inventory.dto.CreateInventoryRequest;
import com.lavaflow.inventory.dto.InventoryResponse;
import com.lavaflow.inventory.entity.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "outlet", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    InventoryItem toEntity(CreateInventoryRequest request);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "outletId", source = "outlet.id")
    @Mapping(target = "outletName", source = "outlet.name")
    @Mapping(target = "menuItemId", source = "menuItem.id")
    @Mapping(target = "menuItemName", source = "menuItem.name")
    @Mapping(target = "availableQuantity", expression = "java(item.getQuantity() - item.getReservedQuantity())")
    @Mapping(target = "lowStock",
            expression = "java(item.getQuantity() - item.getReservedQuantity() <= item.getLowStockThreshold())")
    InventoryResponse toResponse(InventoryItem item);
}