package com.lavaflow.kitchen.repository;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.kitchen.entity.KitchenOrder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class KitchenOrderSpecifications {

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.KITCHEN_PENDING,
            OrderStatus.KITCHEN_ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP
    );

    private KitchenOrderSpecifications() {
    }

    public static Specification<KitchenOrder> filter(
            UUID restaurantId,
            UUID outletId,
            KitchenOrderStatus status,
            boolean activeOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));

            if (outletId != null) {
                predicates.add(cb.equal(root.get("outlet").get("id"), outletId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (activeOnly) {
                predicates.add(root.get("order").get("status").in(ACTIVE_ORDER_STATUSES));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
