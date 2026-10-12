package com.lavaflow.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdjustInventoryRequest {

    @NotNull(message = "Delta is required")
    @Min(value = -1000000, message = "Adjustment is too large")
    @Max(value = 1000000, message = "Adjustment is too large")
    private Integer delta;
}