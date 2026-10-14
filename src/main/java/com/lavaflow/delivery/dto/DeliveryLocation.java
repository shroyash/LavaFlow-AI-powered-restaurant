package com.lavaflow.delivery.dto;

import java.math.BigDecimal;

public record DeliveryLocation(BigDecimal latitude, BigDecimal longitude) {
}