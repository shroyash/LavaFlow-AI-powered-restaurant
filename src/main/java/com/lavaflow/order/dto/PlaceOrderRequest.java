package com.lavaflow.order.dto;

import com.lavaflow.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotNull(message = "Restaurant is required")
    private UUID restaurantId;

    @NotNull(message = "Delivery address is required")
    private UUID addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 500)
    private String specialInstructions;

    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 50, message = "Too many items")
    @Valid
    private List<PlaceOrderItemRequest> items;
}
