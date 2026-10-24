package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.OrderListToolResult;
import com.lavaflow.ai.tool.dto.OrderStatusToolInput;
import com.lavaflow.ai.tool.dto.OrderStatusToolResult;
import com.lavaflow.ai.tool.dto.OrderSummaryToolResult;
import com.lavaflow.ai.tool.dto.PlaceOrderToolInput;
import com.lavaflow.ai.tool.dto.PlaceOrderToolResult;
import com.lavaflow.cart.dto.CartResponse;
import com.lavaflow.cart.service.CartService;
import com.lavaflow.common.enums.PaymentMethod;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.service.CustomerAddressService;
import com.lavaflow.inventory.exception.InsufficientStockException;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.PlaceOrderItemRequest;
import com.lavaflow.order.dto.PlaceOrderRequest;
import com.lavaflow.order.dto.PlaceOrderResult;
import com.lavaflow.order.service.OrderQueryService;
import com.lavaflow.order.service.OrderSubmissionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderTool {

    private final OrderSubmissionService orderSubmissionService;
    private final OrderQueryService orderQueryService;
    private final CartService cartService;
    private final CustomerAddressService customerAddressService;

    public OrderTool(
            OrderSubmissionService orderSubmissionService,
            OrderQueryService orderQueryService,
            CartService cartService,
            CustomerAddressService customerAddressService
    ) {
        this.orderSubmissionService = orderSubmissionService;
        this.orderQueryService = orderQueryService;
        this.cartService = cartService;
        this.customerAddressService = customerAddressService;
    }

    @Tool(description = """
            Retrieve and validate the customer's current cart and order summary before checkout.
            Returns the cart items, restaurant details, subtotal, and saved delivery address.
            Use this when the customer asks for order summary, review, or wants to check out before placing the order.
            """)
    public OrderSummaryToolResult getOrderSummary(ToolContext toolContext) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return OrderSummaryToolResult.failure("Customer authentication context is missing.");
        }

        try {
            CartResponse cart = cartService.getCart(customerId);
            if (cart == null || cart.items() == null || cart.items().isEmpty()) {
                return OrderSummaryToolResult.failure(
                        "Your cart is currently empty. Please select a restaurant and add items to your cart before proceeding."
                );
            }

            List<OrderSummaryToolResult.CartItemSummary> itemSummaries = cart.items().stream()
                    .map(item -> new OrderSummaryToolResult.CartItemSummary(
                            item.menuItemId(),
                            item.name(),
                            item.quantity(),
                            item.unitPrice(),
                            item.lineTotal()
                    ))
                    .toList();

            List<CustomerAddressResponse> addresses = customerAddressService.getAddresses(customerId);
            String deliveryAddress = null;
            UUID deliveryAddressId = null;
            if (addresses != null && !addresses.isEmpty()) {
                CustomerAddressResponse chosen = addresses.stream()
                        .filter(CustomerAddressResponse::isDefaultAddress)
                        .findFirst()
                        .orElse(addresses.get(0));
                deliveryAddress = chosen.getAddressLine();
                deliveryAddressId = UUID.fromString(chosen.getId());
            }

            boolean readyToPlace = deliveryAddressId != null;
            String message = readyToPlace
                    ? "Order summary ready for checkout. Delivery to: " + deliveryAddress
                    : "Cart items ready, but no delivery address is saved. Please specify your delivery address to proceed.";

            return OrderSummaryToolResult.success(
                    message,
                    cart.restaurantId(),
                    cart.restaurantName(),
                    itemSummaries,
                    cart.subtotal(),
                    cart.totalItems(),
                    deliveryAddress,
                    deliveryAddressId,
                    "CASH_ON_DELIVERY",
                    readyToPlace
            );
        } catch (Exception ex) {
            return OrderSummaryToolResult.failure("Failed to retrieve order summary: " + ex.getMessage());
        }
    }

    @Tool(description = """
            Place an order for the customer using items from their cart or provided item list.
            Submits the order with the chosen or default saved delivery address and payment method (default: CASH_ON_DELIVERY).
            Use this when the customer confirms they want to place or checkout their order.
            Never invent order IDs, restaurant IDs, or items.
            """)
    public PlaceOrderToolResult placeOrder(
            PlaceOrderToolInput input,
            ToolContext toolContext
    ) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return PlaceOrderToolResult.failure("Customer authentication context is missing.");
        }

        try {
            // 1. Resolve Delivery Address
            UUID addressId = null;
            if (input != null && input.addressId() != null) {
                addressId = input.addressId();
            } else {
                List<CustomerAddressResponse> addresses = customerAddressService.getAddresses(customerId);
                if (addresses == null || addresses.isEmpty()) {
                    return PlaceOrderToolResult.failure(
                            "No delivery address found. Please provide or save a delivery address before placing your order."
                    );
                }
                CustomerAddressResponse chosen = addresses.stream()
                        .filter(CustomerAddressResponse::isDefaultAddress)
                        .findFirst()
                        .orElse(addresses.get(0));
                addressId = UUID.fromString(chosen.getId());
            }

            // 2. Resolve Restaurant & Items
            UUID restaurantId = null;
            List<PlaceOrderItemRequest> items = null;
            boolean fromCart = false;

            if (input != null && input.items() != null && !input.items().isEmpty()) {
                restaurantId = input.restaurantId() != null ? input.restaurantId() : extractRestaurantId(toolContext);
                items = input.items().stream()
                        .map(i -> {
                            PlaceOrderItemRequest req = new PlaceOrderItemRequest();
                            req.setMenuItemId(i.menuItemId());
                            req.setQuantity(i.quantity());
                            return req;
                        })
                        .toList();
            }

            if (items == null || items.isEmpty()) {
                CartResponse cart = cartService.getCart(customerId);
                if (cart == null || cart.items() == null || cart.items().isEmpty()) {
                    return PlaceOrderToolResult.failure(
                            "Your cart is currently empty. Please add items to your cart before placing an order."
                    );
                }
                restaurantId = cart.restaurantId();
                items = cart.items().stream()
                        .map(i -> {
                            PlaceOrderItemRequest req = new PlaceOrderItemRequest();
                            req.setMenuItemId(i.menuItemId());
                            req.setQuantity(i.quantity());
                            return req;
                        })
                        .toList();
                fromCart = true;
            }

            if (restaurantId == null) {
                restaurantId = extractRestaurantId(toolContext);
            }

            if (restaurantId == null) {
                return PlaceOrderToolResult.failure("No restaurant is selected for this order.");
            }

            // 3. Resolve Payment Method
            PaymentMethod paymentMethod = PaymentMethod.CASH_ON_DELIVERY;
            if (input != null && input.paymentMethod() != null && !input.paymentMethod().isBlank()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(
                            input.paymentMethod().trim().toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_")
                    );
                } catch (IllegalArgumentException ignored) {
                    // Fall back to CASH_ON_DELIVERY
                }
            }

            // 4. Resolve Idempotency Key
            String idempotencyKey;
            if (input != null && input.idempotencyKey() != null && input.idempotencyKey().trim().length() >= 8) {
                idempotencyKey = input.idempotencyKey().trim();
            } else {
                idempotencyKey = UUID.randomUUID().toString();
            }

            PlaceOrderRequest orderRequest = new PlaceOrderRequest();
            orderRequest.setRestaurantId(restaurantId);
            orderRequest.setAddressId(addressId);
            orderRequest.setPaymentMethod(paymentMethod);
            if (input != null && input.specialInstructions() != null) {
                orderRequest.setSpecialInstructions(input.specialInstructions());
            }
            orderRequest.setItems(items);

            PlaceOrderResult result = orderSubmissionService.submit(customerId, idempotencyKey, orderRequest);
            OrderResponse order = result.order();

            if (fromCart) {
                try {
                    cartService.clearCart(customerId);
                } catch (Exception ignored) {
                }
            }

            String msg = result.replayed()
                    ? "Your order was previously submitted and is active."
                    : "Your order #" + order.getOrderNumber() + " has been placed successfully!";

            return PlaceOrderToolResult.success(
                    msg,
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getRestaurantName(),
                    order.getDeliveryAddress(),
                    order.getPaymentMethod()
            );
        } catch (EntityNotFoundException | BadRequestException | ConflictException ex) {
            return PlaceOrderToolResult.failure(ex.getMessage());
        } catch (Exception ex) {
            return PlaceOrderToolResult.failure("Failed to place order: " + ex.getMessage());
        }
    }

    @Tool(description = """
            Retrieve the authenticated customer's recent orders.
            Use this when the customer asks to view past orders, order history, or recent purchases.
            """)
    public OrderListToolResult getMyOrders(ToolContext toolContext) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return OrderListToolResult.failure("Customer authentication context is missing.");
        }

        try {
            var page = orderQueryService.getCustomerOrders(
                    customerId,
                    null,
                    PageRequest.of(0, 5, Sort.by("createdAt").descending())
            );

            if (page.isEmpty()) {
                return OrderListToolResult.empty();
            }

            List<OrderListToolResult.OrderSummaryItem> list = page.getContent().stream()
                    .map(o -> new OrderListToolResult.OrderSummaryItem(
                            o.getId(),
                            o.getOrderNumber(),
                            o.getStatus(),
                            o.getRestaurantName(),
                            o.getTotalAmount(),
                            o.getPaymentMethod(),
                            o.getCreatedAt()
                    ))
                    .toList();

            return OrderListToolResult.success(list);
        } catch (Exception ex) {
            return OrderListToolResult.failure("Failed to retrieve orders: " + ex.getMessage());
        }
    }

    @Tool(description = """
            Retrieve the status and details of a specific order, or the customer's most recent order.
            Use this when the customer asks 'where is my order?', 'check order status', or provides an order ID / order number.
            """)
    public OrderStatusToolResult getOrderStatus(
            OrderStatusToolInput input,
            ToolContext toolContext
    ) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return OrderStatusToolResult.failure("Customer authentication context is missing.");
        }

        try {
            UUID targetOrderId = null;

            if (input != null && input.orderId() != null && !input.orderId().isBlank()) {
                String trimmed = input.orderId().trim();
                try {
                    targetOrderId = UUID.fromString(trimmed);
                } catch (IllegalArgumentException notUuid) {
                    var recent = orderQueryService.getCustomerOrders(
                            customerId,
                            null,
                            PageRequest.of(0, 20, Sort.by("createdAt").descending())
                    );
                    for (var ord : recent.getContent()) {
                        if (trimmed.equalsIgnoreCase(ord.getOrderNumber()) || trimmed.equalsIgnoreCase(ord.getId())) {
                            targetOrderId = UUID.fromString(ord.getId());
                            break;
                        }
                    }
                }
            }

            if (targetOrderId == null) {
                var recent = orderQueryService.getCustomerOrders(
                        customerId,
                        null,
                        PageRequest.of(0, 1, Sort.by("createdAt").descending())
                );
                if (recent.isEmpty()) {
                    return OrderStatusToolResult.failure("You have no placed orders to check.");
                }
                targetOrderId = UUID.fromString(recent.getContent().get(0).getId());
            }

            OrderResponse order = orderQueryService.getCustomerOrder(customerId, targetOrderId);
            return OrderStatusToolResult.success(
                    "Order #" + order.getOrderNumber() + " is currently " + order.getStatus() + ".",
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getRestaurantName(),
                    order.getOutletName(),
                    order.getDeliveryAddress(),
                    order.getCreatedAt()
            );
        } catch (EntityNotFoundException ex) {
            return OrderStatusToolResult.failure("Order not found: " + ex.getMessage());
        } catch (Exception ex) {
            return OrderStatusToolResult.failure("Failed to fetch order status: " + ex.getMessage());
        }
    }

    private UUID extractCustomerId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();
        Object id = context.get("customerId");

        if (id instanceof UUID uuid) {
            return uuid;
        }

        if (id instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }

    private UUID extractRestaurantId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();
        Object id = context.get("restaurantId");

        if (id instanceof UUID uuid) {
            return uuid;
        }

        if (id instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }
}
