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
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.service.CustomerAddressService;
import com.lavaflow.inventory.exception.InsufficientStockException;
import com.lavaflow.order.dto.OrderResponse;
import com.lavaflow.order.dto.OrderSummaryResponse;
import com.lavaflow.order.dto.PlaceOrderRequest;
import com.lavaflow.order.dto.PlaceOrderResult;
import com.lavaflow.order.service.OrderQueryService;
import com.lavaflow.order.service.OrderSubmissionService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTool Unit Tests")
class OrderToolTest {

    @Mock
    private OrderSubmissionService orderSubmissionService;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private CartService cartService;

    @Mock
    private CustomerAddressService customerAddressService;

    private OrderTool orderTool;

    private UUID customerId;
    private UUID restaurantId;
    private UUID addressId;
    private ToolContext toolContext;

    @BeforeEach
    void setUp() {
        orderTool = new OrderTool(
                orderSubmissionService,
                orderQueryService,
                cartService,
                customerAddressService
        );

        customerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        toolContext = new ToolContext(Map.of(
                "customerId", customerId,
                "restaurantId", restaurantId
        ));
    }

    @Test
    @DisplayName("getOrderSummary returns failure when customer context is missing")
    void getOrderSummary_missingCustomerId() {
        OrderSummaryToolResult result = orderTool.getOrderSummary(new ToolContext(Map.of()));
        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("missing");
    }

    @Test
    @DisplayName("getOrderSummary returns failure when cart is empty")
    void getOrderSummary_emptyCart() {
        when(cartService.getCart(customerId)).thenReturn(new CartResponse(
                UUID.randomUUID(), restaurantId, "Test Bistro", List.of(), BigDecimal.ZERO, 0
        ));

        OrderSummaryToolResult result = orderTool.getOrderSummary(toolContext);
        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("empty");
    }

    @Test
    @DisplayName("getOrderSummary returns readyToPlace=true when cart has items and customer has default address")
    void getOrderSummary_successWithAddress() {
        CartResponse.CartItemResponse item = new CartResponse.CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Margherita Pizza", "Fresh basil", null, 2,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(20.00), true
        );
        when(cartService.getCart(customerId)).thenReturn(new CartResponse(
                UUID.randomUUID(), restaurantId, "Italian Kitchen", List.of(item), BigDecimal.valueOf(20.00), 2
        ));

        CustomerAddressResponse addr = CustomerAddressResponse.builder()
                .id(addressId.toString())
                .label("Home")
                .addressLine("123 Food Street, Kathmandu")
                .defaultAddress(true)
                .build();
        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(addr));

        OrderSummaryToolResult result = orderTool.getOrderSummary(toolContext);
        assertThat(result.success()).isTrue();
        assertThat(result.readyToPlace()).isTrue();
        assertThat(result.restaurantName()).isEqualTo("Italian Kitchen");
        assertThat(result.items()).hasSize(1);
        assertThat(result.subtotal()).isEqualByComparingTo("20.00");
        assertThat(result.deliveryAddress()).isEqualTo("123 Food Street, Kathmandu");
    }

    @Test
    @DisplayName("placeOrder places order using cart and default address, clearing cart on success")
    void placeOrder_fromCartSuccess() {
        UUID menuItemId = UUID.randomUUID();
        CartResponse.CartItemResponse item = new CartResponse.CartItemResponse(
                UUID.randomUUID(), menuItemId, "Chicken Pizza", "Yummy", null, 1,
                BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00), true
        );
        when(cartService.getCart(customerId)).thenReturn(new CartResponse(
                UUID.randomUUID(), restaurantId, "City Chicken", List.of(item), BigDecimal.valueOf(15.00), 1
        ));

        CustomerAddressResponse addr = CustomerAddressResponse.builder()
                .id(addressId.toString())
                .label("Home")
                .addressLine("Patan Durbar Square")
                .defaultAddress(true)
                .build();
        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(addr));

        OrderResponse orderResponse = OrderResponse.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-20261024-001")
                .status("PLACED")
                .restaurantName("City Chicken")
                .totalAmount(BigDecimal.valueOf(18.00))
                .deliveryAddress("Patan Durbar Square")
                .paymentMethod("CASH_ON_DELIVERY")
                .build();
        when(orderSubmissionService.submit(eq(customerId), any(), any(PlaceOrderRequest.class)))
                .thenReturn(new PlaceOrderResult(orderResponse, false));

        PlaceOrderToolResult result = orderTool.placeOrder(new PlaceOrderToolInput(
                null, null, null, "Please hurry", null, null
        ), toolContext);

        assertThat(result.success()).isTrue();
        assertThat(result.orderNumber()).isEqualTo("ORD-20261024-001");
        assertThat(result.status()).isEqualTo("PLACED");
        verify(cartService).clearCart(customerId);

        ArgumentCaptor<PlaceOrderRequest> reqCaptor = ArgumentCaptor.forClass(PlaceOrderRequest.class);
        verify(orderSubmissionService).submit(eq(customerId), any(), reqCaptor.capture());
        PlaceOrderRequest captured = reqCaptor.getValue();
        assertThat(captured.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(captured.getAddressId()).isEqualTo(addressId);
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(captured.getSpecialInstructions()).isEqualTo("Please hurry");
        assertThat(captured.getItems()).hasSize(1);
        assertThat(captured.getItems().get(0).getMenuItemId()).isEqualTo(menuItemId);
    }

    @Test
    @DisplayName("placeOrder returns failure if stock is insufficient")
    void placeOrder_insufficientStock() {
        UUID menuItemId = UUID.randomUUID();
        CartResponse.CartItemResponse item = new CartResponse.CartItemResponse(
                UUID.randomUUID(), menuItemId, "Chicken Pizza", "Yummy", null, 1,
                BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00), true
        );
        when(cartService.getCart(customerId)).thenReturn(new CartResponse(
                UUID.randomUUID(), restaurantId, "City Chicken", List.of(item), BigDecimal.valueOf(15.00), 1
        ));

        CustomerAddressResponse addr = CustomerAddressResponse.builder()
                .id(addressId.toString())
                .addressLine("Patan")
                .defaultAddress(true)
                .build();
        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(addr));

        when(orderSubmissionService.submit(eq(customerId), any(), any()))
                .thenThrow(new InsufficientStockException(menuItemId, 0));

        PlaceOrderToolResult result = orderTool.placeOrder(new PlaceOrderToolInput(
                null, null, null, null, null, null
        ), toolContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("out of stock");
    }

    @Test
    @DisplayName("getMyOrders returns list of customer orders")
    void getMyOrders_success() {
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-101")
                .status("DELIVERED")
                .restaurantName("Burger Hub")
                .totalAmount(BigDecimal.valueOf(25.50))
                .paymentMethod("CASH_ON_DELIVERY")
                .createdAt(LocalDateTime.now())
                .build();

        when(orderQueryService.getCustomerOrders(eq(customerId), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        OrderListToolResult result = orderTool.getMyOrders(toolContext);
        assertThat(result.success()).isTrue();
        assertThat(result.orders()).hasSize(1);
        assertThat(result.orders().get(0).orderNumber()).isEqualTo("ORD-101");
    }

    @Test
    @DisplayName("getOrderStatus returns status for most recent order when orderId is omitted")
    void getOrderStatus_mostRecentOrder() {
        UUID recentOrderId = UUID.randomUUID();
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .id(recentOrderId.toString())
                .orderNumber("ORD-202")
                .status("PREPARING")
                .build();

        when(orderQueryService.getCustomerOrders(eq(customerId), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        OrderResponse orderResponse = OrderResponse.builder()
                .id(recentOrderId.toString())
                .orderNumber("ORD-202")
                .status("PREPARING")
                .restaurantName("Wok Box")
                .totalAmount(BigDecimal.valueOf(32.00))
                .build();
        when(orderQueryService.getCustomerOrder(customerId, recentOrderId)).thenReturn(orderResponse);

        OrderStatusToolResult result = orderTool.getOrderStatus(new OrderStatusToolInput(null), toolContext);
        assertThat(result.success()).isTrue();
        assertThat(result.orderNumber()).isEqualTo("ORD-202");
        assertThat(result.status()).isEqualTo("PREPARING");
    }
}
