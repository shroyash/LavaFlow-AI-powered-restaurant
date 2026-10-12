package com.lavaflow.inventory.service;

import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.inventory.dto.InventoryResponse;
import com.lavaflow.inventory.dto.UpdateInventoryRequest;
import com.lavaflow.inventory.entity.InventoryItem;
import com.lavaflow.inventory.mapper.InventoryMapper;
import com.lavaflow.inventory.repository.InventoryItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse updateStock(UUID restaurantId, UUID inventoryId, UpdateInventoryRequest request) {
        int reserved = findOwned(restaurantId, inventoryId).getReservedQuantity();

        int updated = inventoryItemRepository.updateStock(
                inventoryId,
                restaurantId,
                request.getQuantity(),
                request.getLowStockThreshold(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new ConflictException(
                    "Quantity cannot be lower than the " + reserved + " units reserved for open orders."
            );
        }

        return inventoryMapper.toResponse(findOwned(restaurantId, inventoryId));
    }

    @Transactional
    public InventoryResponse adjustStock(UUID restaurantId, UUID inventoryId, int delta) {
        int reserved = findOwned(restaurantId, inventoryId).getReservedQuantity();

        int updated = inventoryItemRepository.adjustQuantity(inventoryId, restaurantId, delta, LocalDateTime.now());

        if (updated == 0) {
            throw new ConflictException(
                    "Stock cannot go below the " + reserved + " units reserved for open orders."
            );
        }

        return inventoryMapper.toResponse(findOwned(restaurantId, inventoryId));
    }

    private InventoryItem findOwned(UUID restaurantId, UUID inventoryId) {
        return inventoryItemRepository.findByIdAndRestaurantId(inventoryId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Stock record not found."));
    }
}