package com.lavaflow.inventory.service;

import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.exception.InsufficientStockException;
import com.lavaflow.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveAll(UUID outletId, List<StockLine> lines) {
        LocalDateTime now = LocalDateTime.now();

        mergeAndSort(lines).forEach((menuItemId, quantity) -> {
            if (inventoryItemRepository.reserve(outletId, menuItemId, quantity, now) == 0) {
                int available = inventoryItemRepository.findAvailableQuantity(outletId, menuItemId).orElse(0);
                throw new InsufficientStockException(menuItemId, Math.max(available, 0));
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseAll(UUID outletId, List<StockLine> lines) {
        LocalDateTime now = LocalDateTime.now();

        mergeAndSort(lines).forEach((menuItemId, quantity) -> {
            if (inventoryItemRepository.release(outletId, menuItemId, quantity, now) == 0) {
                throw new IllegalStateException("Reservation mismatch for menu item " + menuItemId);
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeAll(UUID outletId, List<StockLine> lines) {
        LocalDateTime now = LocalDateTime.now();

        mergeAndSort(lines).forEach((menuItemId, quantity) -> {
            if (inventoryItemRepository.consume(outletId, menuItemId, quantity, now) == 0) {
                throw new IllegalStateException("Reservation mismatch for menu item " + menuItemId);
            }
        });
    }

    private Map<UUID, Integer> mergeAndSort(List<StockLine> lines) {
        return lines.stream().collect(Collectors.toMap(
                StockLine::menuItemId,
                StockLine::quantity,
                Integer::sum,
                TreeMap::new
        ));
    }
}