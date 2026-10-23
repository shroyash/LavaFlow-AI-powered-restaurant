
package com.lavaflow.cart.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.entity.Cart;
import com.lavaflow.cart.entity.CartItem;
import com.lavaflow.cart.repository.CartItemRepository;
import com.lavaflow.cart.repository.CartRepository;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private CartService cartService;

    private UUID customerId;
    private UUID restaurantId;
    private UUID menuItemId;

    private User customer;
    private Restaurant restaurant;
    private MenuItem menuItem;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartRepository,
                cartItemRepository,
                userRepository,
                menuItemRepository,
                restaurantRepository
        );

        customerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        customer = new User();
        ReflectionTestUtils.setField(customer, "id", customerId);
        customer.setRole(UserRole.CUSTOMER);
        customer.setActive(true);

        restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", restaurantId);
        restaurant.setName("Test Restaurant");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        menuItem = new MenuItem();
        ReflectionTestUtils.setField(menuItem, "id", menuItemId);
        menuItem.setName("Chicken Momo");
        menuItem.setDescription("Steamed chicken dumplings");
        menuItem.setPrice(new BigDecimal("180.00"));
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);

        cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", UUID.randomUUID());
        cart.setCustomer(customer);
        cart.setRestaurant(restaurant);
    }

    private void stubCustomer() {
        when(userRepository.findById(customerId))
                .thenReturn(Optional.of(customer));
    }

    private void stubMenuItem() {
        when(menuItemRepository.findById(menuItemId))
                .thenReturn(Optional.of(menuItem));
    }

    private void stubCartSave() {
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addItemShouldCalculateSubtotalUsingDatabasePrice() {
        stubCustomer();
        stubMenuItem();
        stubCartSave();

        when(cartRepository.findWithItemsByCustomerId(customerId))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.addItem(customerId, menuItemId, 2);

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(
                new BigDecimal("180.00"),
                response.items().get(0).unitPrice()
        );
        assertEquals(
                new BigDecimal("360.00"),
                response.subtotal()
        );
        assertEquals(2, response.totalItems());
    }

    @Test
    void addItemShouldIncreaseQuantityWhenItemAlreadyExists() {
        stubCustomer();
        stubMenuItem();
        stubCartSave();

        CartItem existingItem = new CartItem();
        existingItem.setMenuItem(menuItem);
        existingItem.setQuantity(2);
        cart.addItem(existingItem);

        when(cartRepository.findWithItemsByCustomerId(customerId))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.addItem(customerId, menuItemId, 3);

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(
                new BigDecimal("900.00"),
                response.subtotal()
        );
        assertEquals(5, response.totalItems());
    }

    @Test
    void addItemShouldRejectItemsFromDifferentRestaurants() {
        stubCustomer();

        Restaurant anotherRestaurant = new Restaurant();
        UUID anotherRestaurantId = UUID.randomUUID();

        ReflectionTestUtils.setField(
                anotherRestaurant, "id", anotherRestaurantId
        );
        anotherRestaurant.setName("Another Restaurant");
        anotherRestaurant.setStatus(RestaurantStatus.ACTIVE);

        MenuItem anotherItem = new MenuItem();
        UUID anotherMenuItemId = UUID.randomUUID();

        ReflectionTestUtils.setField(
                anotherItem, "id", anotherMenuItemId
        );
        anotherItem.setName("Pizza");
        anotherItem.setPrice(new BigDecimal("350.00"));
        anotherItem.setAvailable(true);
        anotherItem.setRestaurant(anotherRestaurant);

        CartItem existingItem = new CartItem();
        existingItem.setMenuItem(menuItem);
        existingItem.setQuantity(1);
        cart.addItem(existingItem);

        when(menuItemRepository.findById(anotherMenuItemId))
                .thenReturn(Optional.of(anotherItem));

        when(cartRepository.findWithItemsByCustomerId(customerId))
                .thenReturn(Optional.of(cart));

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(
                        customerId, anotherMenuItemId, 1
                )
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemShouldRejectUnavailableMenuItem() {
        stubCustomer();
        stubMenuItem();

        menuItem.setAvailable(false);

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(customerId, menuItemId, 1)
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemShouldRejectInactiveRestaurant() {
        stubCustomer();
        stubMenuItem();

        // Replace INACTIVE if your enum uses a different non-active value.
        restaurant.setStatus(RestaurantStatus.INACTIVE);

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(customerId, menuItemId, 1)
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemShouldRejectQuantityAboveMaximum() {
        assertThrows(
                BadRequestException.class,
                () -> cartService.addItem(customerId, menuItemId, 101)
        );

        verify(userRepository, never()).findById(any());
        verify(menuItemRepository, never()).findById(any());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemShouldRejectZeroQuantity() {
        assertThrows(
                BadRequestException.class,
                () -> cartService.addItem(customerId, menuItemId, 0)
        );

        verify(userRepository, never()).findById(any());
        verify(menuItemRepository, never()).findById(any());
        verify(cartRepository, never()).save(any(Cart.class));
    }
}
