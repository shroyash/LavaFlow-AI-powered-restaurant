package com.lavaflow.inventory.service;

import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryAvailabilityService {

    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public Set<UUID> findOutletsWithStock(Collection<UUID> outletIds, List<StockLine> lines) {
        if (outletIds.isEmpty()) {
            return Set.of();
        }
        if (lines == null || lines.isEmpty()) {
            return new HashSet<>(outletIds);
        }

        Map<UUID, Integer> required = lines.stream().collect(Collectors.toMap(
                StockLine::menuItemId,
                StockLine::quantity,
                Integer::sum
        ));

        Map<UUID, Map<UUID, Integer>> availableByOutlet = new HashMap<>();
        inventoryItemRepository.findAvailability(outletIds, required.keySet()).forEach(row ->
                availableByOutlet
                        .computeIfAbsent(row.getOutletId(), outletId -> new HashMap<>())
                        .put(row.getMenuItemId(), row.getAvailable())
        );

        return outletIds.stream()
                .filter(outletId -> {
                    Map<UUID, Integer> available = availableByOutlet.getOrDefault(outletId, Map.of());
                    return required.entrySet().stream()
                            .allMatch(entry -> available.getOrDefault(entry.getKey(), 0) >= entry.getValue());
                })
                .collect(Collectors.toSet());
    }
}