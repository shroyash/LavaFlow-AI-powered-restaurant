package com.lavaflow.kitchen.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KitchenOrderItemResponse {

    private String itemName;
    private int quantity;
}
