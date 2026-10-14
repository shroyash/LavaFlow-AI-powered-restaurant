package com.lavaflow.delivery.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.delivery.dto.DeliveryLocation;
import com.lavaflow.delivery.dto.NearbyRestaurantRequest;
import com.lavaflow.delivery.dto.NearbyRestaurantResponse;
import com.lavaflow.delivery.mapper.NearbyRestaurantMapper;
import com.lavaflow.delivery.service.DeliveryCoverage.Candidate;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NearbyRestaurantService {

    private static final int DEFAULT_LIMIT = 10;

    private final OutletRepository outletRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final DeliveryLocationResolver deliveryLocationResolver;
    private final DeliveryCoverage deliveryCoverage;
    private final NearbyRestaurantMapper nearbyRestaurantMapper;

    @Transactional(readOnly = true)
    public List<NearbyRestaurantResponse> locate(UUID customerId, NearbyRestaurantRequest request) {
        DeliveryLocation location = deliveryLocationResolver.resolve(
                customerId, request.getAddressId(), request.getLatitude(), request.getLongitude()
        );
        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_LIMIT;

        return findNearby(location, request.getQuery(), limit);
    }

    @Transactional(readOnly = true)
    public List<NearbyRestaurantResponse> findNearby(DeliveryLocation location, String query, int limit) {
        Set<UUID> matchingRestaurantIds = matchingRestaurantIds(query);

        List<Outlet> outlets = outletRepository.findDeliverableOutlets(
                        RestaurantStatus.ACTIVE,
                        List.of(OutletStatus.ACTIVE, OutletStatus.BUSY)
                ).stream()
                .filter(outlet -> matchingRestaurantIds == null
                        || matchingRestaurantIds.contains(outlet.getRestaurant().getId()))
                .toList();

        return deliveryCoverage.inRange(outlets, location.latitude(), location.longitude()).stream()
                .collect(Collectors.groupingBy(candidate -> candidate.outlet().getRestaurant().getId()))
                .values().stream()
                .map(deliveryCoverage::pickBest)
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparing((Candidate candidate) -> candidate.outlet().getStatus() == OutletStatus.BUSY)
                        .thenComparingDouble(Candidate::distanceKm))
                .limit(limit)
                .map(candidate -> nearbyRestaurantMapper.toResponse(
                        candidate.outlet().getRestaurant(),
                        candidate.outlet(),
                        deliveryCoverage.roundKm(candidate.distanceKm()),
                        deliveryCoverage.estimateMinutes(candidate.distanceKm())
                ))
                .toList();
    }

    private Set<UUID> matchingRestaurantIds(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";

        Set<UUID> ids = new HashSet<>(restaurantRepository.findIdsByNamePattern(RestaurantStatus.ACTIVE, pattern));
        ids.addAll(menuItemRepository.findRestaurantIdsByAvailableItemName(pattern));
        return ids;
    }
}