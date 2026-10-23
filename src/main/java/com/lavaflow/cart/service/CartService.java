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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 100;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public CartResponse addItem(
            UUID customerId,
            UUID menuItemId,
            int quantity
    ) {
        validateQuantity(quantity);

        User customer = getCustomer(customerId);
        MenuItem menuItem = getOrderableMenuItem(menuItemId);

        Restaurant restaurant = menuItem.getRestaurant();

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> createCart(customer));

        validateRestaurantConsistency(cart, restaurant);

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int updatedQuantity = existingItem.getQuantity() + quantity;

            validateQuantity(updatedQuantity);
            existingItem.setQuantity(updatedQuantity);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setMenuItem(menuItem);
            cartItem.setQuantity(quantity);

            cart.addItem(cartItem);
        }

        cart.setRestaurant(restaurant);

        Cart savedCart = cartRepository.save(cart);

        return toResponse(savedCart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID customerId) {
        getCustomer(customerId);

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Cart not found. Add an item to create your cart."
                        )
                );

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(
            UUID customerId,
            UUID menuItemId,
            int quantity
    ) {
        validateQuantity(quantity);
        getCustomer(customerId);

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart not found.")
                );

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Item not found in your cart."
                        )
                );

        // Revalidate availability and restaurant status at modification time.
        getOrderableMenuItem(menuItemId);

        cartItem.setQuantity(quantity);

        Cart savedCart = cartRepository.save(cart);

        return toResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItem(
            UUID customerId,
            UUID menuItemId
    ) {
        getCustomer(customerId);

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart not found.")
                );

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Item not found in your cart."
                        )
                );

        cart.removeItem(cartItem);

        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(null);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID customerId) {
        getCustomer(customerId);

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart not found.")
                );

        cart.clearItems();
        cart.setRestaurant(null);

        return toResponse(cartRepository.save(cart));
    }

    private User getCustomer(UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Customer not found.")
                );

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException(
                    "Only customer accounts can manage a cart."
            );
        }

        if (!customer.isActive()) {
            throw new BadRequestException(
                    "This customer account is inactive."
            );
        }

        return customer;
    }

    private MenuItem getOrderableMenuItem(UUID menuItemId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Menu item not found.")
                );

        if (!menuItem.isAvailable()) {
            throw new ConflictException(
                    "Menu item '" + menuItem.getName()
                            + "' is currently unavailable."
            );
        }

        Restaurant restaurant = menuItem.getRestaurant();

        if (restaurant == null
                || restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new ConflictException(
                    "This menu item's restaurant is currently inactive."
            );
        }

        return menuItem;
    }

    private Cart createCart(User customer) {
        Cart cart = new Cart();
        cart.setCustomer(customer);

        return cart;
    }

    private void validateRestaurantConsistency(
            Cart cart,
            Restaurant requestedRestaurant
    ) {
        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(requestedRestaurant);
            return;
        }

        Restaurant currentRestaurant = cart.getRestaurant();

        if (currentRestaurant == null
                || !currentRestaurant.getId()
                .equals(requestedRestaurant.getId())) {
            throw new ConflictException(
                    "Your cart already contains items from another restaurant. "
                            + "Clear your cart before selecting a different restaurant."
            );
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException(
                    "Quantity must be between 1 and "
                            + MAX_QUANTITY_PER_ITEM + "."
            );
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses =
                new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem cartItem : cart.getItems()) {
            MenuItem menuItem = cartItem.getMenuItem();

            BigDecimal unitPrice = menuItem.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(cartItem.getQuantity())
            );

            boolean available = menuItem.isAvailable()
                    && menuItem.getRestaurant() != null
                    && menuItem.getRestaurant().getStatus()
                    == RestaurantStatus.ACTIVE;

            itemResponses.add(
                    new CartResponse.CartItemResponse(
                            cartItem.getId(),
                            menuItem.getId(),
                            menuItem.getName(),
                            menuItem.getDescription(),
                            menuItem.getImageUrl(),
                            cartItem.getQuantity(),
                            unitPrice,
                            lineTotal,
                            available
                    )
            );

            subtotal = subtotal.add(lineTotal);
            totalItems += cartItem.getQuantity();
        }

        Restaurant restaurant = cart.getRestaurant();

        return new CartResponse(
                cart.getId(),
                restaurant != null ? restaurant.getId() : null,
                restaurant != null ? restaurant.getName() : null,
                List.copyOf(itemResponses),
                subtotal,
                totalItems
        );
    }
}