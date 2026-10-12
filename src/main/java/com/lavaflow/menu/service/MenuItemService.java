package com.lavaflow.menu.service;

import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.common.storage.PublicImageStorage;
import com.lavaflow.menu.dto.CreateMenuItemRequest;
import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.dto.UpdateMenuItemRequest;
import com.lavaflow.menu.entity.MenuCategory;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.mapper.MenuItemMapper;
import com.lavaflow.menu.repository.MenuCategoryRepository;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.menu.repository.MenuItemSpecifications;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private static final String DUPLICATE_NAME_MESSAGE = "An item with this name already exists in this category.";

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemMapper menuItemMapper;
    private final PublicImageStorage publicImageStorage;

    @Transactional
    public MenuItemResponse createItem(UUID restaurantId, CreateMenuItemRequest request) {
        MenuCategory category = findOwnedCategory(restaurantId, request.getCategoryId());
        String name = request.getName().trim();

        if (menuItemRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        MenuItem item = menuItemMapper.toEntity(request);
        item.setRestaurant(restaurant);
        item.setCategory(category);
        item.setName(name);

        return menuItemMapper.toResponse(menuItemRepository.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getItems(
            UUID restaurantId,
            UUID categoryId,
            Boolean available,
            String query,
            Pageable pageable
    ) {
        return menuItemRepository
                .findAll(MenuItemSpecifications.filter(restaurantId, categoryId, available, query), pageable)
                .map(menuItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getItem(UUID restaurantId, UUID itemId) {
        return menuItemMapper.toResponse(findOwnedItem(restaurantId, itemId));
    }

    @Transactional
    public MenuItemResponse updateItem(UUID restaurantId, UUID itemId, UpdateMenuItemRequest request) {
        MenuItem item = findOwnedItem(restaurantId, itemId);
        MenuCategory category = findOwnedCategory(restaurantId, request.getCategoryId());
        String name = request.getName().trim();

        if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(category.getId(), name, itemId)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        menuItemMapper.updateEntity(request, item);
        item.setCategory(category);
        item.setName(name);

        return menuItemMapper.toResponse(menuItemRepository.saveAndFlush(item));
    }

    @Transactional
    public void deleteItem(UUID restaurantId, UUID itemId) {
        MenuItem item = findOwnedItem(restaurantId, itemId);

        try {
            menuItemRepository.delete(item);
            menuItemRepository.flush();
            publicImageStorage.deleteOnCommit(item.getImageUrl());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "This item is referenced by existing orders or stock records. Mark it as unavailable instead."
            );
        }
    }

    private MenuItem findOwnedItem(UUID restaurantId, UUID itemId) {
        return menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found."));
    }

    private MenuCategory findOwnedCategory(UUID restaurantId, UUID categoryId) {
        return menuCategoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found."));
    }
}