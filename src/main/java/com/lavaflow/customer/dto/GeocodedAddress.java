package com.lavaflow.customer.dto;

import java.math.BigDecimal;

public record GeocodedAddress(
        BigDecimal latitude,
        BigDecimal longitude,
        String displayName
) {
}