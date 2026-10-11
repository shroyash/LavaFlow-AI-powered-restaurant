package com.lavaflow.menu.service;

import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.menu.dto.CreateMenuCategoryRequest;
import com.lavaflow.menu.dto.MenuCategoryResponse;
import com.lavaflow.menu.dto.UpdateMenuCategoryRequest;
import com.lavaflow.menu.entity.MenuCategory;
import com.lavaflow.menu.mapper.MenuCategoryMapper;
import com.lavaflow.menu.repository.MenuCategoryRepository;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private static final String DUPLICATE_NAME_MESSAGE = "A category with this name already exists.";

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryMapper menuCategoryMapper;

    @Transactional
    public MenuCategoryResponse createCategory(UUID restaurantId, CreateMenuCategoryRequest request) {
        String name = request.getName().trim();

        if (menuCategoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found."));

        MenuCategory category = menuCategoryMapper.toEntity(request);
        category.setRestaurant(restaurant);
        category.setName(name);

        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(menuCategoryRepository.findMaxDisplayOrder(restaurantId) + 1);
        }

        return menuCategoryMapper.toResponse(menuCategoryRepository.saveAndFlush(category));
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategories(UUID restaurantId, Boolean active) {
        List<MenuCategory> categories = active == null
                ? menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAscNameAsc(restaurantId)
                : menuCategoryRepository.findByRestaurantIdAndActiveOrderByDisplayOrderAscNameAsc(restaurantId, active);

        return categories.stream().map(menuCategoryMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategory(UUID restaurantId, UUID categoryId) {
        return menuCategoryMapper.toResponse(findOwnedCategory(restaurantId, categoryId));
    }

    @Transactional
    public MenuCategoryResponse updateCategory(UUID restaurantId, UUID categoryId, UpdateMenuCategoryRequest request) {
        MenuCategory category = findOwnedCategory(restaurantId, categoryId);
        String name = request.getName().trim();

        if (menuCategoryRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, name, categoryId)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        menuCategoryMapper.updateEntity(request, category);
        category.setName(name);

        return menuCategoryMapper.toResponse(menuCategoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void deleteCategory(UUID restaurantId, UUID categoryId) {
        MenuCategory category = findOwnedCategory(restaurantId, categoryId);

        if (menuItemRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("This category still has menu items. Move or delete them first.");
        }

        menuCategoryRepository.delete(category);
    }

    private MenuCategory findOwnedCategory(UUID restaurantId, UUID categoryId) {
        return menuCategoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found."));
    }
}