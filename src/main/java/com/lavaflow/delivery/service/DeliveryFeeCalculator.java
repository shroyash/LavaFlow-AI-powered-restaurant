package com.lavaflow.delivery.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryFeeCalculator {

    private static final BigDecimal BASE_FEE = BigDecimal.valueOf(2.00);
    private static final BigDecimal PER_KM_FEE = BigDecimal.valueOf(0.50);

    public BigDecimal calculate(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BASE_FEE.setScale(2, RoundingMode.HALF_UP);
        }
        return BASE_FEE.add(PER_KM_FEE.multiply(distanceKm)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculate(double distanceKm) {
        return calculate(BigDecimal.valueOf(distanceKm));
    }
}
