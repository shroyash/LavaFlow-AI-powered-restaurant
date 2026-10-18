package com.lavaflow.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectKitchenOrderRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500)
    private String reason;
}
