package com.lavaflow.restaurant.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.restaurant.dto.KitchenStaffResponse;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.mapper.KitchenStaffMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantStaffQueryService Tests")
class RestaurantStaffQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KitchenStaffMapper kitchenStaffMapper;

    @InjectMocks
    private RestaurantStaffQueryService queryService;

    private UUID restaurantId;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Test Restaurant");
    }

    @Test
    @DisplayName("Should return only staff members (excluding RESTAURANT_OWNER and others)")
    void shouldFilterStaffRoles() {
        User staffUser = new User();
        staffUser.setId(UUID.randomUUID());
        staffUser.setFullName("Chef One");
        staffUser.setRole(UserRole.KITCHEN_STAFF);
        staffUser.setRestaurant(restaurant);

        User ownerUser = new User();
        ownerUser.setId(UUID.randomUUID());
        ownerUser.setFullName("Owner Person");
        ownerUser.setRole(UserRole.RESTAURANT_OWNER);
        ownerUser.setRestaurant(restaurant);

        when(userRepository.findByRestaurantId(restaurantId)).thenReturn(List.of(staffUser, ownerUser));
        when(kitchenStaffMapper.toResponse(staffUser)).thenReturn(
                KitchenStaffResponse.builder()
                        .userId(staffUser.getId().toString())
                        .fullName("Chef One")
                        .role("KITCHEN_STAFF")
                        .restaurantId(restaurantId.toString())
                        .active(true)
                        .build()
        );

        List<KitchenStaffResponse> result = queryService.getRestaurantStaff(restaurantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Chef One");
    }

    @Test
    @DisplayName("Should return staff member by id when user belongs to restaurant")
    void shouldReturnStaffMemberById() {
        UUID staffId = UUID.randomUUID();
        User staffUser = new User();
        staffUser.setId(staffId);
        staffUser.setFullName("Chef Two");
        staffUser.setRole(UserRole.KITCHEN_STAFF);
        staffUser.setRestaurant(restaurant);

        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));
        when(kitchenStaffMapper.toResponse(staffUser)).thenReturn(
                KitchenStaffResponse.builder()
                        .userId(staffId.toString())
                        .fullName("Chef Two")
                        .role("KITCHEN_STAFF")
                        .restaurantId(restaurantId.toString())
                        .active(true)
                        .build()
        );

        KitchenStaffResponse response = queryService.getStaffMember(restaurantId, staffId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(staffId.toString());
        assertThat(response.getFullName()).isEqualTo("Chef Two");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException if staff belongs to another restaurant")
    void shouldThrowWhenCrossTenantAccess() {
        UUID otherRestaurantId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(otherRestaurantId);

        User staffUser = new User();
        staffUser.setId(staffId);
        staffUser.setRole(UserRole.KITCHEN_STAFF);
        staffUser.setRestaurant(otherRestaurant);

        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> queryService.getStaffMember(restaurantId, staffId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Staff member not found.");
    }
}

