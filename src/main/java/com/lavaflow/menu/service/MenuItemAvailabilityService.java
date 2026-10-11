package com.lavaflow.menu.service;

import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.mapper.MenuItemMapper;
import com.lavaflow.menu.repository.MenuItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemAvailabilityService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    @Transactional
    public MenuItemResponse changeAvailability(UUID restaurantId, UUID itemId, boolean available) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found."));

        item.setAvailable(available);

        return menuItemMapper.toResponse(menuItemRepository.save(item));
    }
}