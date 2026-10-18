package com.lavaflow.kitchen.mapper;

import com.lavaflow.common.mapper.DateTimeMapper;
import com.lavaflow.kitchen.dto.KitchenOrderItemResponse;
import com.lavaflow.kitchen.dto.KitchenOrderResponse;
import com.lavaflow.kitchen.entity.KitchenOrder;
import com.lavaflow.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = DateTimeMapper.class)
public interface KitchenOrderMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "outletId", source = "outlet.id")
    @Mapping(target = "outletName", source = "outlet.name")
    @Mapping(target = "items", source = "order.items")
    @Mapping(target = "specialInstructions", source = "order.specialInstructions")
    KitchenOrderResponse toResponse(KitchenOrder kitchenOrder);

    KitchenOrderItemResponse toItemResponse(OrderItem item);
}
