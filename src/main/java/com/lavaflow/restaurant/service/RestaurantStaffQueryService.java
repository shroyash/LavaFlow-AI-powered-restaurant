package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import com.lavaflow.restaurant.mapper.KitchenStaffMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantStaffQueryService {

    /**
     * Roles that count as "staff" for a restaurant.
     * RESTAURANT_OWNER is the admin themselves; excluded from the staff list.
     */
    private static final Set<UserRole> STAFF_ROLES = Set.of(
            UserRole.KITCHEN_STAFF,
            UserRole.RESTAURANT_STAFF,
            UserRole.OUTLET_MANAGER
    );

    private final UserRepository userRepository;
    private final KitchenStaffMapper kitchenStaffMapper;

    /**
     * Returns all staff members belonging to the given restaurant,
     * filtering out roles that are not considered "staff" (e.g. RESTAURANT_OWNER, SUPER_ADMIN).
     */
    @Transactional(readOnly = true)
    public List<KitchenStaffResponse> getRestaurantStaff(UUID restaurantId) {
        return userRepository.findByRestaurantId(restaurantId)
                .stream()
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .map(kitchenStaffMapper::toResponse)
                .toList();
    }

    /**
     * Returns a single staff member by ID, scoped to the restaurant.
     * Throws EntityNotFoundException if the user is not found or does not belong to the restaurant.
     */
    @Transactional(readOnly = true)
    public KitchenStaffResponse getStaffMember(UUID restaurantId, UUID staffId) {
        User user = userRepository.findById(staffId)
                .filter(u -> u.getRestaurant() != null && restaurantId.equals(u.getRestaurant().getId()))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new EntityNotFoundException("Staff member not found."));
        return kitchenStaffMapper.toResponse(user);
    }
}

