package com.lavaflow.inventory.service;

import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.inventory.dto.CreateInventoryRequest;
import com.lavaflow.inventory.dto.InventoryResponse;
import com.lavaflow.inventory.entity.InventoryItem;
import com.lavaflow.inventory.mapper.InventoryMapper;
import com.lavaflow.inventory.repository.InventoryItemRepository;
import com.lavaflow.inventory.repository.InventorySpecifications;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.outlet.entity.Outlet;
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
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final OutletRepository outletRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse createInventory(UUID restaurantId, CreateInventoryRequest request) {
        Outlet outlet = outletRepository.findByIdAndRestaurantId(request.getOutletId(), restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Outlet not found."));

        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(request.getMenuItemId(), restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found."));

        if (inventoryItemRepository.existsByOutletIdAndMenuItemId(outlet.getId(), menuItem.getId())) {
            throw new ConflictException("A stock record already exists for this item at this outlet.");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        InventoryItem inventoryItem = inventoryMapper.toEntity(request);
        inventoryItem.setRestaurant(restaurant);
        inventoryItem.setOutlet(outlet);
        inventoryItem.setMenuItem(menuItem);

        return inventoryMapper.toResponse(inventoryItemRepository.saveAndFlush(inventoryItem));
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventory(
            UUID restaurantId,
            UUID outletId,
            UUID menuItemId,
            Boolean lowStockOnly,
            Pageable pageable
    ) {
        return inventoryItemRepository
                .findAll(InventorySpecifications.filter(restaurantId, outletId, menuItemId, lowStockOnly), pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryItem(UUID restaurantId, UUID inventoryId) {
        return inventoryMapper.toResponse(findOwned(restaurantId, inventoryId));
    }

    @Transactional
    public void deleteInventory(UUID restaurantId, UUID inventoryId) {
        findOwned(restaurantId, inventoryId);

        if (inventoryItemRepository.deleteUnreserved(inventoryId, restaurantId) == 0) {
            throw new ConflictException("This stock record has units reserved for open orders and cannot be removed.");
        }
    }

    private InventoryItem findOwned(UUID restaurantId, UUID inventoryId) {
        return inventoryItemRepository.findByIdAndRestaurantId(inventoryId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Stock record not found."));
    }
}