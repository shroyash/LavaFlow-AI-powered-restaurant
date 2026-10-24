package com.lavaflow.ai.tool;

import com.lavaflow.customer.service.AddressGeocodingService;
import com.lavaflow.customer.service.CustomerAddressService;
import com.lavaflow.delivery.service.DeliveryLocationResolver;
import com.lavaflow.delivery.service.NearbyRestaurantService;
import com.lavaflow.menu.service.MenuCategoryService;
import com.lavaflow.menu.service.MenuItemService;
import com.lavaflow.outlet.repository.OutletRepository;
import com.lavaflow.restaurant.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Spring AI Tool Registration & Schema Tests")
class ToolRegistrationTest {

    @Mock
    private MenuItemService menuItemService;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuCategoryService menuCategoryService;

    @Mock
    private CustomerAddressService customerAddressService;

    @Mock
    private NearbyRestaurantService nearbyRestaurantService;

    @Mock
    private DeliveryLocationResolver deliveryLocationResolver;

    @Mock
    private AddressGeocodingService addressGeocodingService;

    @Mock
    private com.lavaflow.order.service.OrderSubmissionService orderSubmissionService;

    @Mock
    private com.lavaflow.order.service.OrderQueryService orderQueryService;

    @Mock
    private com.lavaflow.cart.service.CartService cartService;

    @Mock
    private OutletRepository outletRepository;

    @Test
    @DisplayName("MenuSearchTool registers with Spring AI and excludes ToolContext from model input schema")
    void menuSearchTool_registersCorrectly() {
        MenuSearchTool tool = new MenuSearchTool(menuItemService);
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        assertThat(callbacks).hasSize(1);
        ToolCallback callback = callbacks[0];

        assertThat(callback.getToolDefinition().name()).isEqualTo("searchMenu");
        assertThat(callback.getToolDefinition().description()).containsIgnoringCase("selected restaurant");

        // ToolContext parameter must NOT be exposed in the tool input schema
        String schema = callback.getToolDefinition().inputSchema();
        assertThat(schema).doesNotContain("toolContext");
        assertThat(schema).doesNotContain("ToolContext");
        assertThat(schema).doesNotContain("restaurantId");
        assertThat(schema).contains("query");
    }

    @Test
    @DisplayName("RestaurantDiscoveryTool registers listAvailableRestaurants and getMenuCategories without ToolContext leaking")
    void restaurantDiscoveryTool_registersCorrectly() {
        RestaurantDiscoveryTool tool = new RestaurantDiscoveryTool(restaurantService, menuCategoryService, outletRepository);
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        List<String> names = Arrays.stream(callbacks)
                .map(c -> c.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder("listAvailableRestaurants", "getMenuCategories");

        ToolCallback categoriesCallback = Arrays.stream(callbacks)
                .filter(c -> c.getToolDefinition().name().equals("getMenuCategories"))
                .findFirst()
                .orElseThrow();

        // getMenuCategories takes ONLY ToolContext, so input schema must not expose any parameters
        String schema = categoriesCallback.getToolDefinition().inputSchema();
        assertThat(schema).doesNotContain("toolContext");
        assertThat(schema).doesNotContain("ToolContext");
        assertThat(schema).doesNotContain("restaurantId");
    }

    @Test
    @DisplayName("CustomerAddressTool registers listSavedAddresses and saveDeliveryAddress without leaking customerId or coordinates")
    void customerAddressTool_registersCorrectly() {
        CustomerAddressTool tool = new CustomerAddressTool(customerAddressService);
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        List<String> names = Arrays.stream(callbacks)
                .map(c -> c.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder("listSavedAddresses", "saveDeliveryAddress");

        ToolCallback listCallback = Arrays.stream(callbacks)
                .filter(c -> c.getToolDefinition().name().equals("listSavedAddresses"))
                .findFirst()
                .orElseThrow();
        String listSchema = listCallback.getToolDefinition().inputSchema();
        assertThat(listSchema).doesNotContain("toolContext");
        assertThat(listSchema).doesNotContain("customerId");

        ToolCallback saveCallback = Arrays.stream(callbacks)
                .filter(c -> c.getToolDefinition().name().equals("saveDeliveryAddress"))
                .findFirst()
                .orElseThrow();
        String saveSchema = saveCallback.getToolDefinition().inputSchema();
        assertThat(saveSchema).doesNotContain("toolContext");
        assertThat(saveSchema).doesNotContain("customerId");
        assertThat(saveSchema).doesNotContain("latitude");
        assertThat(saveSchema).doesNotContain("longitude");
        assertThat(saveSchema).contains("addressLine");
    }

    @Test
    @DisplayName("NearbyRestaurantTool registers findNearbyRestaurants without leaking customerId or coordinates")
    void nearbyRestaurantTool_registersCorrectly() {
        NearbyRestaurantTool tool = new NearbyRestaurantTool(
                nearbyRestaurantService,
                deliveryLocationResolver,
                addressGeocodingService,
                customerAddressService
        );
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        assertThat(callbacks).hasSize(1);
        ToolCallback callback = callbacks[0];

        assertThat(callback.getToolDefinition().name()).isEqualTo("findNearbyRestaurants");
        String schema = callback.getToolDefinition().inputSchema();
        assertThat(schema).doesNotContain("toolContext");
        assertThat(schema).doesNotContain("customerId");
        assertThat(schema).doesNotContain("latitude");
        assertThat(schema).doesNotContain("longitude");
        assertThat(schema).contains("addressId");
        assertThat(schema).contains("manualAddressLine");
        assertThat(schema).contains("query");
    }

    @Test
    @DisplayName("OrderTool registers getOrderSummary, placeOrder, getMyOrders, and getOrderStatus correctly")
    void orderTool_registersCorrectly() {
        OrderTool tool = new OrderTool(
                orderSubmissionService,
                orderQueryService,
                cartService,
                customerAddressService
        );
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        List<String> names = Arrays.stream(callbacks)
                .map(c -> c.getToolDefinition().name())
                .toList();

        assertThat(names).containsExactlyInAnyOrder(
                "getOrderSummary",
                "placeOrder",
                "getMyOrders",
                "getOrderStatus"
        );

        for (ToolCallback callback : callbacks) {
            String schema = callback.getToolDefinition().inputSchema();
            assertThat(schema).doesNotContain("toolContext");
            assertThat(schema).doesNotContain("ToolContext");
            assertThat(schema).doesNotContain("customerId");
        }
    }
}

