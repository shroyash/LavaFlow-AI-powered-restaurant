package com.lavaflow.order.dto;

public record PlaceOrderResult(OrderResponse order, boolean replayed) {
}
