package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CategoryListResult;
import com.lavaflow.ai.tool.dto.MenuCategoryToolResponse;
import com.lavaflow.ai.tool.dto.RestaurantToolResponse;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.menu.dto.MenuCategoryResponse;
import com.lavaflow.menu.service.MenuCategoryService;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.dto.RestaurantResponse;
import com.lavaflow.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantDiscoveryTool Tests")
class RestaurantDiscoveryToolTest {

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuCategoryService menuCategoryService;

    @Mock
    private OutletRepository outletRepository;

    private RestaurantDiscoveryTool discoveryTool;

    @BeforeEach
    void setUp() {
        discoveryTool = new RestaurantDiscoveryTool(restaurantService, menuCategoryService, outletRepository);
    }

    @Test
    @DisplayName("listAvailableRestaurants requests only ACTIVE restaurants with pagination limit 10")
    void listAvailableRestaurants_activeOnly() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantResponse response = RestaurantResponse.builder()
                .id(restaurantId.toString())
                .name("Pizza Bella")
                .description("Authentic Italian Pizzas")
                .phone("+1234567890") // Internal details that must NOT be exposed
                .email("bella@example.com")
                .status("ACTIVE")
                .build();

        when(restaurantService.getRestaurants(eq(RestaurantStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        List<RestaurantToolResponse> results = discoveryTool.listAvailableRestaurants();

        assertThat(results).hasSize(1);
        RestaurantToolResponse item = results.get(0);
        assertThat(item.id()).isEqualTo(restaurantId);
        assertThat(item.name()).isEqualTo("Pizza Bella");
        assertThat(item.description()).isEqualTo("Authentic Italian Pizzas");
        assertThat(item.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(restaurantService).getRestaurants(eq(RestaurantStatus.ACTIVE), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("listAvailableRestaurants with ToolContext populates capture with real outletId")
    @SuppressWarnings("unchecked")
    void listAvailableRestaurants_withCapture_populatesOutletId() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantResponse response = RestaurantResponse.builder()
                .id(restaurantId.toString())
                .name("City Chicken")
                .build();

        when(restaurantService.getRestaurants(eq(RestaurantStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));
        // Simulate no outlets found — capture should still work
        when(outletRepository.findByRestaurantIdAndStatusIn(eq(restaurantId), any(Collection.class)))
                .thenReturn(List.of());

        NearbyRestaurantResultCapture capture = new NearbyRestaurantResultCapture();
        ToolContext ctx = new ToolContext(Map.of("nearbyRestaurantResultCapture", capture));

        discoveryTool.listAvailableRestaurants(ctx);

        assertThat(capture.getRestaurants()).hasSize(1);
        assertThat(capture.getRestaurants().get(0).restaurantId()).isEqualTo(restaurantId);
        assertThat(capture.getRestaurants().get(0).restaurantName()).isEqualTo("City Chicken");
    }

    @Test
    @DisplayName("getMenuCategories fails closed when ToolContext has no restaurantId")
    void getMenuCategories_noRestaurantInContext_returnsWarning() {
        ToolContext context = new ToolContext(Map.of("customerId", UUID.randomUUID()));

        CategoryListResult result = discoveryTool.getMenuCategories(context);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("no restaurant is currently selected");
        assertThat(result.categories()).isEmpty();
        verifyNoInteractions(menuCategoryService);
    }

    @Test
    @DisplayName("getMenuCategories delegates with exact restaurantId and active=true")
    void getMenuCategories_validContext_delegatesToService() {
        UUID restaurantId = UUID.randomUUID();
        ToolContext context = new ToolContext(Map.of("restaurantId", restaurantId));

        MenuCategoryResponse cat1 = MenuCategoryResponse.builder()
                .name("Starters")
                .description("Appetizers and soups")
                .build();

        MenuCategoryResponse cat2 = MenuCategoryResponse.builder()
                .name("Desserts")
                .description("Sweet treats")
                .build();

        when(menuCategoryService.getCategories(restaurantId, true))
                .thenReturn(List.of(cat1, cat2));

        CategoryListResult result = discoveryTool.getMenuCategories(context);

        assertThat(result.success()).isTrue();
        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories()).extracting(MenuCategoryToolResponse::name)
                .containsExactly("Starters", "Desserts");

        verify(menuCategoryService).getCategories(restaurantId, true);
    }
}


