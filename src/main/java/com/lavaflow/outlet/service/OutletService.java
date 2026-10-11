package com.lavaflow.outlet.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.outlet.dto.CreateOutletRequest;
import com.lavaflow.outlet.dto.OutletResponse;
import com.lavaflow.outlet.dto.UpdateOutletRequest;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.outlet.mapper.OutletMapper;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutletService {

    private final OutletRepository outletRepository;
    private final RestaurantRepository restaurantRepository;
    private final OutletMapper outletMapper;

    @Transactional
    public OutletResponse createOutlet(UUID restaurantId, CreateOutletRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        Outlet outlet = outletMapper.toEntity(request);
        outlet.setRestaurant(restaurant);
        outlet.setName(outlet.getName().trim());
        outlet.setAddress(outlet.getAddress().trim());

        return outletMapper.toResponse(outletRepository.save(outlet));
    }

    @Transactional(readOnly = true)
    public Page<OutletResponse> getOutlets(UUID restaurantId, OutletStatus status, Pageable pageable) {
        Page<Outlet> outlets = status == null
                ? outletRepository.findByRestaurantId(restaurantId, pageable)
                : outletRepository.findByRestaurantIdAndStatus(restaurantId, status, pageable);
        return outlets.map(outletMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OutletResponse getOutlet(UUID restaurantId, UUID outletId) {
        return outletMapper.toResponse(findOwnedOutlet(restaurantId, outletId));
    }

    @Transactional
    public OutletResponse updateOutlet(UUID restaurantId, UUID outletId, UpdateOutletRequest request) {
        Outlet outlet = findOwnedOutlet(restaurantId, outletId);

        outletMapper.updateEntity(request, outlet);
        outlet.setName(outlet.getName().trim());
        outlet.setAddress(outlet.getAddress().trim());

        return outletMapper.toResponse(outletRepository.save(outlet));
    }

    private Outlet findOwnedOutlet(UUID restaurantId, UUID outletId) {
        return outletRepository.findByIdAndRestaurantId(outletId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Outlet not found."));
    }
}