package com.lavaflow.delivery.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.geo.GeoDistanceCalculator;
import com.lavaflow.customer.entity.CustomerAddress;
import com.lavaflow.customer.repository.CustomerAddressRepository;
import com.lavaflow.delivery.config.DeliveryProperties;
import com.lavaflow.delivery.dto.NearestOutletItemRequest;
import com.lavaflow.delivery.dto.NearestOutletRequest;
import com.lavaflow.delivery.dto.NearestOutletResponse;
import com.lavaflow.delivery.exception.LocatorFailureReason;
import com.lavaflow.delivery.exception.OutletLocatorException;
import com.lavaflow.delivery.mapper.NearestOutletMapper;
import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.service.InventoryAvailabilityService;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutletLocatorService {

    private final RestaurantRepository restaurantRepository;
    private final OutletRepository outletRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final DeliveryProperties deliveryProperties;
    private final NearestOutletMapper nearestOutletMapper;

    @Transactional(readOnly = true)
    public NearestOutletResponse locate(UUID customerId, NearestOutletRequest request) {
        boolean hasAddress = request.getAddressId() != null;
        boolean hasCoordinates = request.getLatitude() != null && request.getLongitude() != null;

        if (hasAddress == hasCoordinates) {
            throw new BadRequestException("Provide either an address id or both latitude and longitude.");
        }

        List<StockLine> items = toStockLines(request.getItems());

        if (hasAddress) {
            return findNearestForAddress(customerId, request.getRestaurantId(), request.getAddressId(), items);
        }
        return findNearest(request.getRestaurantId(), request.getLatitude(), request.getLongitude(), items);
    }

    @Transactional(readOnly = true)
    public NearestOutletResponse findNearestForAddress(
            UUID customerId,
            UUID restaurantId,
            UUID addressId,
            List<StockLine> items
    ) {
        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found."));

        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new OutletLocatorException(
                    LocatorFailureReason.ADDRESS_NEEDS_LOCATION,
                    "This address has no map location. Please share your location or pick the spot on the map."
            );
        }

        return findNearest(restaurantId, address.getLatitude(), address.getLongitude(), items);
    }

    @Transactional(readOnly = true)
    public NearestOutletResponse findNearest(
            UUID restaurantId,
            BigDecimal latitude,
            BigDecimal longitude,
            List<StockLine> items
    ) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new OutletLocatorException(
                    LocatorFailureReason.RESTAURANT_NOT_AVAILABLE,
                    "This restaurant is not accepting orders right now."
            );
        }

        List<Candidate> inRange = outletRepository
                .findByRestaurantIdAndStatusIn(restaurantId, List.of(OutletStatus.ACTIVE, OutletStatus.BUSY))
                .stream()
                .filter(outlet -> outlet.getLatitude() != null && outlet.getLongitude() != null)
                .map(outlet -> new Candidate(outlet, distanceKm(outlet, latitude, longitude)))
                .filter(candidate -> candidate.distanceKm() <= radiusKm(candidate.outlet()))
                .toList();

        if (inRange.isEmpty()) {
            throw new OutletLocatorException(
                    LocatorFailureReason.NO_OUTLET_IN_RANGE,
                    "Sorry, none of our outlets deliver to this location."
            );
        }

        Set<UUID> outletsWithStock = inventoryAvailabilityService.findOutletsWithStock(
                inRange.stream().map(candidate -> candidate.outlet().getId()).toList(),
                items
        );

        List<Candidate> fulfilling = inRange.stream()
                .filter(candidate -> outletsWithStock.contains(candidate.outlet().getId()))
                .toList();

        if (fulfilling.isEmpty()) {
            throw new OutletLocatorException(
                    LocatorFailureReason.ITEMS_UNAVAILABLE,
                    "The nearby outlets do not have all the requested items in stock."
            );
        }

        Candidate best = closest(fulfilling, OutletStatus.ACTIVE)
                .or(() -> closest(fulfilling, OutletStatus.BUSY))
                .orElseThrow();

        return nearestOutletMapper.toResponse(
                best.outlet(),
                BigDecimal.valueOf(best.distanceKm()).setScale(1, RoundingMode.HALF_UP),
                estimateMinutes(best.distanceKm())
        );
    }

    private Optional<Candidate> closest(List<Candidate> candidates, OutletStatus status) {
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

    private int estimateMinutes(double distanceKm) {
        double travelMinutes = distanceKm / deliveryProperties.getAverageSpeedKmh() * 60;
        return (int) Math.ceil(deliveryProperties.getPrepMinutes() + travelMinutes);
    }

    private List<StockLine> toStockLines(List<NearestOutletItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new StockLine(item.getMenuItemId(), item.getQuantity()))
                .toList();
    }

    private record Candidate(Outlet outlet, double distanceKm) {
    }
}