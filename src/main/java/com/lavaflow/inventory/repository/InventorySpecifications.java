package com.lavaflow.inventory.repository;

import com.lavaflow.inventory.entity.InventoryItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<InventoryItem> filter(
            UUID restaurantId,
            UUID outletId,
            UUID menuItemId,
            Boolean lowStockOnly
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));

            if (outletId != null) {
                predicates.add(cb.equal(root.get("outlet").get("id"), outletId));
            }
            if (menuItemId != null) {
                predicates.add(cb.equal(root.get("menuItem").get("id"), menuItemId));
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(
                        cb.diff(root.<Integer>get("quantity"), root.<Integer>get("reservedQuantity")),
                        root.<Integer>get("lowStockThreshold")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}