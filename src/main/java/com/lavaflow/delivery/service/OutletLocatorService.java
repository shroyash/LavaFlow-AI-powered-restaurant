package com.lavaflow.delivery.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.delivery.dto.DeliveryLocation;
import com.lavaflow.delivery.dto.NearestOutletItemRequest;
import com.lavaflow.delivery.dto.NearestOutletRequest;
import com.lavaflow.delivery.dto.NearestOutletResponse;
import com.lavaflow.delivery.enums.LocatorFailureReason;
import com.lavaflow.delivery.exception.OutletLocatorException;
import com.lavaflow.delivery.mapper.NearestOutletMapper;
import com.lavaflow.delivery.service.DeliveryCoverage.Candidate;
import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.service.InventoryAvailabilityService;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutletLocatorService {

    private final RestaurantRepository restaurantRepository;
    private final OutletRepository outletRepository;
    private final DeliveryLocationResolver deliveryLocationResolver;
    private final DeliveryCoverage deliveryCoverage;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final NearestOutletMapper nearestOutletMapper;

    @Transactional(readOnly = true)
    public NearestOutletResponse locate(UUID customerId, NearestOutletRequest request) {
        DeliveryLocation location = deliveryLocationResolver.resolve(
                customerId, request.getAddressId(), request.getLatitude(), request.getLongitude()
        );
        return findNearest(
                request.getRestaurantId(),
                location.latitude(),
                location.longitude(),
                toStockLines(request.getItems())
        );
    }

    @Transactional(readOnly = true)
    public NearestOutletResponse findNearestForAddress(
            UUID customerId,
            UUID restaurantId,
            UUID addressId,
            List<StockLine> items
    ) {
        DeliveryLocation location = deliveryLocationResolver.resolveAddress(customerId, addressId);
        return findNearest(restaurantId, location.latitude(), location.longitude(), items);
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

        List<Candidate> inRange = deliveryCoverage.inRange(
                outletRepository.findByRestaurantIdAndStatusIn(
                        restaurantId, List.of(OutletStatus.ACTIVE, OutletStatus.BUSY)
                ),
                latitude,
                longitude
        );

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

        Candidate best = deliveryCoverage.pickBest(fulfilling).orElseThrow();

        return nearestOutletMapper.toResponse(
                best.outlet(),
                deliveryCoverage.roundKm(best.distanceKm()),
                deliveryCoverage.estimateMinutes(best.distanceKm())
        );
    }

    private List<StockLine> toStockLines(List<NearestOutletItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new StockLine(item.getMenuItemId(), item.getQuantity()))
                .toList();
    }
}