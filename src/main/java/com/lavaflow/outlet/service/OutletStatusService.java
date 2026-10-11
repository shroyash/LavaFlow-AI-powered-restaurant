package com.lavaflow.outlet.service;

import com.lavaflow.common.enums.OutletStatus;
import com.lavaflow.outlet.dto.OutletResponse;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.outlet.mapper.OutletMapper;
import com.lavaflow.outlet.repository.OutletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutletStatusService {

    private final OutletRepository outletRepository;
    private final OutletMapper outletMapper;

    @Transactional
    public OutletResponse changeStatus(UUID restaurantId, UUID outletId, OutletStatus status) {
        Outlet outlet = outletRepository.findByIdAndRestaurantId(outletId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Outlet not found."));

        outlet.setStatus(status);

        return outletMapper.toResponse(outletRepository.save(outlet));
    }
}