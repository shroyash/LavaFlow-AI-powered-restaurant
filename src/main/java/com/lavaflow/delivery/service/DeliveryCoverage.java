package com.lavaflow.delivery.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.common.geo.GeoDistanceCalculator;
import com.lavaflow.delivery.config.DeliveryProperties;
import com.lavaflow.outlet.entity.Outlet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeliveryCoverage {

    private final DeliveryProperties deliveryProperties;

    public record Candidate(Outlet outlet, double distanceKm) {
    }

    public List<Candidate> inRange(Collection<Outlet> outlets, BigDecimal latitude, BigDecimal longitude) {
        return outlets.stream()
                .filter(outlet -> outlet.getLatitude() != null && outlet.getLongitude() != null)
                .map(outlet -> new Candidate(outlet, distanceKm(outlet, latitude, longitude)))
                .filter(candidate -> candidate.distanceKm() <= radiusKm(candidate.outlet()))
                .toList();
    }

    public Optional<Candidate> pickBest(Collection<Candidate> candidates) {
        return closest(candidates, OutletStatus.ACTIVE)
                .or(() -> closest(candidates, OutletStatus.BUSY));
    }

    public int estimateMinutes(double distanceKm) {
        double travelMinutes = distanceKm / deliveryProperties.getAverageSpeedKmh() * 60;
        return (int) Math.ceil(deliveryProperties.getPrepMinutes() + travelMinutes);
    }

    public BigDecimal roundKm(double distanceKm) {
        return BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP);
    }

    private Optional<Candidate> closest(Collection<Candidate> candidates, OutletStatus status) {
        return candidates.stream()
                .filter(candidate -> candidate.outlet().getStatus() == status)
                .min(Comparator.comparingDouble(Candidate::distanceKm));
    }

    private double distanceKm(Outlet outlet, BigDecimal latitude, BigDecimal longitude) {
        return GeoDistanceCalculator.distanceKm(
                latitude.doubleValue(),
                longitude.doubleValue(),
                outlet.getLatitude().doubleValue(),
                outlet.getLongitude().doubleValue()
        );
    }

    private double radiusKm(Outlet outlet) {
        return outlet.getDeliveryRadiusKm() != null
                ? outlet.getDeliveryRadiusKm().doubleValue()
                : deliveryProperties.getDefaultRadiusKm();
    }
}