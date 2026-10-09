package com.lavaflow.restaurant.dto;

import com.lavaflow.common.enums.RestaurantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRestaurantStatusRequest {

    @NotNull(message = "Status is required")
    private RestaurantStatus status;
}