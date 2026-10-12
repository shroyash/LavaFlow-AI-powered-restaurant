package com.lavaflow.menu.service;

import com.lavaflow.common.storage.PublicImageStorage;
import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.mapper.MenuItemMapper;
import com.lavaflow.menu.repository.MenuItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemImageService {

    private final MenuItemRepository menuItemRepository;
    private final PublicImageStorage publicImageStorage;
    private final MenuItemMapper menuItemMapper;

    @Transactional
    public MenuItemResponse uploadImage(UUID restaurantId, UUID itemId, MultipartFile file) {
        MenuItem item = findOwnedItem(restaurantId, itemId);
        String previousImage = item.getImageUrl();

        item.setImageUrl(publicImageStorage.store("menu-items/" + restaurantId, file));
        MenuItem saved = menuItemRepository.save(item);

        publicImageStorage.deleteOnCommit(previousImage);
        return menuItemMapper.toResponse(saved);
    }

    @Transactional
    public MenuItemResponse removeImage(UUID restaurantId, UUID itemId) {
        MenuItem item = findOwnedItem(restaurantId, itemId);
        String previousImage = item.getImageUrl();

        item.setImageUrl(null);
        MenuItem saved = menuItemRepository.save(item);

        publicImageStorage.deleteOnCommit(previousImage);
        return menuItemMapper.toResponse(saved);
    }

    private MenuItem findOwnedItem(UUID restaurantId, UUID itemId) {
        return menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found."));
    }
}