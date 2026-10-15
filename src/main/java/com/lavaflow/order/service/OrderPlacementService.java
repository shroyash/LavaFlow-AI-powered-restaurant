package com.lavaflow.order.service;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.common.enums.PaymentMethod;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.customer.entity.CustomerAddress;
import com.lavaflow.customer.repository.CustomerAddressRepository;
import com.lavaflow.delivery.dto.NearestOutletResponse;
import com.lavaflow.delivery.service.DeliveryFeeCalculator;
import com.lavaflow.delivery.service.OutletLocatorService;
import com.lavaflow.inventory.dto.StockLine;
import com.lavaflow.inventory.exception.InsufficientStockException;
import com.lavaflow.inventory.service.InventoryReservationService;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.menu.repository.MenuItemRepository;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.PlaceOrderItemRequest;
import com.lavaflow.order.dto.PlaceOrderRequest;
import com.lavaflow.order.entity.Order;
import com.lavaflow.order.entity.OrderItem;
import com.lavaflow.order.event.OrderPlacedEvent;
import com.lavaflow.order.mapper.OrderMapper;
import com.lavaflow.order.repository.OrderRepository;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.outlet.repository.OutletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPlacementService {

    private static final int MAX_QUANTITY_PER_ITEM = 100;

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OutletRepository outletRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;
    private final OutletLocatorService outletLocatorService;
    private final InventoryReservationService inventoryReservationService;
    private final DeliveryFeeCalculator deliveryFeeCalculator;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse placeOrder(
            UUID customerId,
            String idempotencyKey,
            String requestHash,
            PlaceOrderRequest request
    ) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));

        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new BadRequestException("Please add a phone number to your profile before ordering.");
        }

        List<StockLine> lines = mergeLines(request.getItems());
        Map<UUID, MenuItem> menuItems = loadOrderableItems(request.getRestaurantId(), lines);

        CustomerAddress address = customerAddressRepository
                .findByIdAndCustomerId(request.getAddressId(), customerId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found."));

        NearestOutletResponse nearest = outletLocatorService.findNearestForAddress(
                customerId, request.getRestaurantId(), request.getAddressId(), lines
        );
        UUID outletId = UUID.fromString(nearest.getOutletId());

        Outlet outlet = outletRepository.findByIdAndRestaurantId(outletId, request.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Outlet not found."));

        reserveStock(outletId, lines, menuItems);

        Order order = new Order();
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestHash(requestHash);
        order.setCustomer(customer);
        order.setRestaurant(outlet.getRestaurant());
        order.setOutlet(outlet);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryAddress(address.getAddressLine());
        order.setDeliveryLatitude(address.getLatitude());
        order.setDeliveryLongitude(address.getLongitude());
        order.setSpecialInstructions(blankToNull(request.getSpecialInstructions()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (StockLine line : lines) {
            MenuItem menuItem = menuItems.get(line.menuItemId());
            BigDecimal lineSubtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(line.quantity()));

            OrderItem item = new OrderItem();
            item.setMenuItem(menuItem);
            item.setItemName(menuItem.getName());
            item.setUnitPrice(menuItem.getPrice());
            item.setQuantity(line.quantity());
            item.setSubtotal(lineSubtotal);
            order.addItem(item);

            subtotal = subtotal.add(lineSubtotal);
        }

        BigDecimal deliveryFee = deliveryFeeCalculator.calculate(nearest.getDistanceKm());
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotalAmount(subtotal.add(deliveryFee));

        order.setStatus(request.getPaymentMethod() == PaymentMethod.COD
                ? OrderStatus.PENDING_KITCHEN
                : OrderStatus.CREATED);

        Order saved = orderRepository.saveAndFlush(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                saved.getId(),
                request.getRestaurantId(),
                outletId,
                saved.getStatus(),
                saved.getPaymentMethod(),
                saved.getTotalAmount()
        ));

        return orderMapper.toResponse(saved);
    }

    private List<StockLine> mergeLines(List<PlaceOrderItemRequest> items) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        items.forEach(item -> merged.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum));

        merged.forEach((menuItemId, quantity) -> {
            if (quantity > MAX_QUANTITY_PER_ITEM) {
                throw new BadRequestException(
                        "You can order at most " + MAX_QUANTITY_PER_ITEM + " of the same item."
                );
            }
        });

        return merged.entrySet().stream()
                .map(entry -> new StockLine(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<UUID, MenuItem> loadOrderableItems(UUID restaurantId, List<StockLine> lines) {
        List<UUID> ids = lines.stream().map(StockLine::menuItemId).toList();
        List<MenuItem> found = menuItemRepository.findByRestaurantIdAndIdIn(restaurantId, ids);

        if (found.size() != ids.size()) {
            throw new BadRequestException("One or more items are not on this restaurant's menu.");
        }

        found.forEach(item -> {
            if (!item.isAvailable() || !item.getCategory().isActive()) {
                throw new ConflictException(item.getName() + " is currently unavailable.");
            }
        });

        return found.stream().collect(Collectors.toMap(MenuItem::getId, Function.identity()));
    }

    private void reserveStock(UUID outletId, List<StockLine> lines, Map<UUID, MenuItem> menuItems) {
        try {
            inventoryReservationService.reserveAll(outletId, lines);
        } catch (InsufficientStockException e) {
            throw new ConflictException(menuItems.get(e.getMenuItemId()).getName() + ": " + e.getMessage());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}