package com.lavaflow.auth.security;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.common.security.SecurityUtils;
import com.lavaflow.restaurant.entity.Restaurant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Authorization and Multi-Tenant Isolation Tests")
class AuthorizationAndTenantTest {

    private UUID restaurantAId;
    private UUID restaurantBId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        restaurantAId = UUID.randomUUID();
        restaurantBId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UserRole role, UUID restaurantId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user-" + role.name() + "@lavaflow.com");
        user.setRole(role);
        user.setActive(true);

        if (restaurantId != null) {
            Restaurant restaurant = new Restaurant();
            restaurant.setId(restaurantId);
            user.setRestaurant(restaurant);
        }

        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("10. SUPER_ADMIN can access all restaurants (cross-tenant by design)")
    void superAdminCanAccessAllRestaurants() {
        authenticate(UserRole.SUPER_ADMIN, null);

        assertThat(SecurityUtils.isSuperAdmin()).isTrue();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantAId)).isTrue();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantBId)).isTrue();
        assertThat(SecurityUtils.getCurrentRestaurantId()).isNull();
    }

    @Test
    @DisplayName("11. RESTAURANT_ADMIN can only access own restaurant")
    void restaurantAdminCanOnlyAccessOwnRestaurant() {
        authenticate(UserRole.RESTAURANT_ADMIN, restaurantAId);

        assertThat(SecurityUtils.isSuperAdmin()).isFalse();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantAId)).isTrue();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantBId)).isFalse();
        assertThat(SecurityUtils.getCurrentRestaurantId()).isEqualTo(restaurantAId);
    }

    @Test
    @DisplayName("12. Cross-tenant access forbidden: Restaurant A staff cannot access Restaurant B")
    void crossTenantAccessForbidden() {
        authenticate(UserRole.RESTAURANT_STAFF, restaurantAId);

        assertThat(SecurityUtils.belongsToRestaurant(restaurantAId)).isTrue();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantBId)).isFalse();
    }

    @Test
    @DisplayName("13. Tenant context derived strictly from SecurityContext (frontend cannot override)")
    void tenantDerivedFromSecurityContext() {
        authenticate(UserRole.RESTAURANT_ADMIN, restaurantAId);

        // Even if frontend passed restaurantBId in query/body, SecurityUtils returns restaurantAId
        UUID resolvedRestaurantId = SecurityUtils.getCurrentRestaurantId();
        assertThat(resolvedRestaurantId).isEqualTo(restaurantAId);
        assertThat(resolvedRestaurantId).isNotEqualTo(restaurantBId);
    }

    @Test
    @DisplayName("14. CUSTOMER can access own resources with null restaurant_id")
    void customerCanAccessWithoutRestaurantId() {
        authenticate(UserRole.CUSTOMER, null);

        assertThat(SecurityUtils.getCurrentUserRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(SecurityUtils.getCurrentRestaurantId()).isNull();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantAId)).isFalse();
    }

    @Test
    @DisplayName("15. Unauthenticated request throws exception on requireCurrentUser")
    void unauthenticatedThrowsException() {
        SecurityContextHolder.clearContext();

        assertThat(SecurityUtils.getCurrentUser()).isEmpty();
        assertThat(SecurityUtils.isSuperAdmin()).isFalse();
        assertThat(SecurityUtils.belongsToRestaurant(restaurantAId)).isFalse();
        assertThatThrownBy(SecurityUtils::requireCurrentUser)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }
}
