package com.lavaflow.order.mapper;

import com.lavaflow.order.dto.OrderItemResponse;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.OrderSummaryResponse;
import com.lavaflow.order.entity.Order;
import com.lavaflow.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "outletId", source = "outlet.id")
    @Mapping(target = "outletName", source = "outlet.name")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    OrderResponse toResponse(Order order);

    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "outletName", source = "outlet.name")
    OrderSummaryResponse toSummary(Order order);

    @Mapping(target = "menuItemId", source = "menuItem.id")
    OrderItemResponse toItemResponse(OrderItem item);
}
