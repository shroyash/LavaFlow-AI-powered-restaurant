package com.lavaflow.menu.repository;

import com.lavaflow.menu.entity.MenuItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MenuItemSpecifications {

    private static final char ESCAPE = '\\';

    private MenuItemSpecifications() {
    }

    public static Specification<MenuItem> filter(UUID restaurantId, UUID categoryId, Boolean available, String query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (available != null) {
                predicates.add(cb.equal(root.get("available"), available));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern, ESCAPE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}