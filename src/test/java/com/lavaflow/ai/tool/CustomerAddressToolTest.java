package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CustomerAddressListResult;
import com.lavaflow.ai.tool.dto.CustomerAddressToolResponse;
import com.lavaflow.ai.tool.dto.SaveAddressInput;
import com.lavaflow.ai.tool.dto.SaveAddressResult;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.service.CustomerAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressTool Unit Tests")
class CustomerAddressToolTest {

    @Mock
    private CustomerAddressService customerAddressService;

    private CustomerAddressTool tool;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        tool = new CustomerAddressTool(customerAddressService);
        customerId = UUID.randomUUID();
    }

    private ToolContext createContext(UUID custId) {
        if (custId == null) {
            return new ToolContext(Map.of());
        }
        return new ToolContext(Map.of("customerId", custId));
    }

    @Test
    @DisplayName("listSavedAddresses fails closed when customerId is missing")
    void listSavedAddresses_missingCustomerId_failsClosed() {
        CustomerAddressListResult result = tool.listSavedAddresses(createContext(null));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.addresses()).isEmpty();
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("listSavedAddresses fails closed when customerId in ToolContext is malformed string")
    void listSavedAddresses_malformedCustomerIdString_failsClosed() {
        ToolContext malformedContext = new ToolContext(Map.of("customerId", "not-a-valid-uuid"));
        CustomerAddressListResult result = tool.listSavedAddresses(malformedContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.addresses()).isEmpty();
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("listSavedAddresses fails closed when customerId in ToolContext is unexpected type")
    void listSavedAddresses_unexpectedCustomerIdType_failsClosed() {
        ToolContext unexpectedTypeContext = new ToolContext(Map.of("customerId", 12345));
        CustomerAddressListResult result = tool.listSavedAddresses(unexpectedTypeContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.addresses()).isEmpty();
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("listSavedAddresses returns allowlisted customer addresses")
    void listSavedAddresses_success() {
        UUID addressId = UUID.randomUUID();
        CustomerAddressResponse response = CustomerAddressResponse.builder()
                .id(addressId.toString())
                .label("Home")
                .addressLine("123 Main St, Kathmandu")
                .defaultAddress(true)
                .latitude(new BigDecimal("27.7172"))
                .longitude(new BigDecimal("85.3240"))
                .build();

        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(response));

        CustomerAddressListResult result = tool.listSavedAddresses(createContext(customerId));

        assertThat(result.success()).isTrue();
        assertThat(result.addresses()).hasSize(1);
        CustomerAddressToolResponse item = result.addresses().get(0);
        assertThat(item.addressId()).isEqualTo(addressId);
        assertThat(item.label()).isEqualTo("Home");
        assertThat(item.addressLine()).isEqualTo("123 Main St, Kathmandu");
        assertThat(item.defaultAddress()).isTrue();
    }

    @Test
    @DisplayName("saveDeliveryAddress fails closed when customerId is missing")
    void saveDeliveryAddress_missingCustomerId_failsClosed() {
        SaveAddressInput input = new SaveAddressInput("Kathmandu Durbar Square", "Office");
        SaveAddressResult result = tool.saveDeliveryAddress(input, createContext(null));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("saveDeliveryAddress rejects null or empty address line")
    void saveDeliveryAddress_emptyAddress_rejected() {
        SaveAddressResult resultNull = tool.saveDeliveryAddress(new SaveAddressInput(null, "Office"), createContext(customerId));
        assertThat(resultNull.success()).isFalse();
        assertThat(resultNull.message()).contains("Address line must not be empty");

        SaveAddressResult resultBlank = tool.saveDeliveryAddress(new SaveAddressInput("   ", "Office"), createContext(customerId));
        assertThat(resultBlank.success()).isFalse();
        assertThat(resultBlank.message()).contains("Address line must not be empty");
    }

    @Test
    @DisplayName("saveDeliveryAddress rejects oversized address line")
    void saveDeliveryAddress_oversizedAddress_rejected() {
        String longAddress = "A".repeat(501);
        SaveAddressResult result = tool.saveDeliveryAddress(new SaveAddressInput(longAddress, "Office"), createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("exceeds the maximum allowed length");
    }

    @Test
    @DisplayName("saveDeliveryAddress rejects oversized label")
    void saveDeliveryAddress_oversizedLabel_rejected() {
        String longLabel = "L".repeat(51);
        SaveAddressResult result = tool.saveDeliveryAddress(new SaveAddressInput("Jhamsikhel, Lalitpur", longLabel), createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Address label exceeds the maximum allowed length");
    }

    @Test
    @DisplayName("saveDeliveryAddress delegates to service with coordinates nullified and returns allowlisted response")
    void saveDeliveryAddress_success() {
        UUID newAddressId = UUID.randomUUID();
        SaveAddressInput input = new SaveAddressInput("Patan Durbar Square, Lalitpur", "Work");

        CustomerAddressResponse savedResponse = CustomerAddressResponse.builder()
                .id(newAddressId.toString())
                .label("Work")
                .addressLine("Patan Durbar Square, Lalitpur")
                .defaultAddress(false)
                .latitude(new BigDecimal("27.6744"))
                .longitude(new BigDecimal("85.3245"))
                .build();

        when(customerAddressService.createAddress(eq(customerId), any(CreateCustomerAddressRequest.class)))
                .thenReturn(savedResponse);

        SaveAddressResult result = tool.saveDeliveryAddress(input, createContext(customerId));

        assertThat(result.success()).isTrue();
        assertThat(result.address()).isNotNull();
        assertThat(result.address().addressId()).isEqualTo(newAddressId);
        assertThat(result.address().label()).isEqualTo("Work");
        assertThat(result.address().addressLine()).isEqualTo("Patan Durbar Square, Lalitpur");

        ArgumentCaptor<CreateCustomerAddressRequest> captor = ArgumentCaptor.forClass(CreateCustomerAddressRequest.class);
        verify(customerAddressService).createAddress(eq(customerId), captor.capture());
        CreateCustomerAddressRequest capturedReq = captor.getValue();
        assertThat(capturedReq.getAddressLine()).isEqualTo("Patan Durbar Square, Lalitpur");
        assertThat(capturedReq.getLabel()).isEqualTo("Work");
        // Coordinates must NOT be provided by the tool/model
        assertThat(capturedReq.getLatitude()).isNull();
        assertThat(capturedReq.getLongitude()).isNull();
    }

    @Test
    @DisplayName("saveDeliveryAddress handles geocoding BadRequestException cleanly")
    void saveDeliveryAddress_geocodingError_returnsCleanMessage() {
        SaveAddressInput input = new SaveAddressInput("Invalid Unknown Place 12345", null);

        when(customerAddressService.createAddress(eq(customerId), any(CreateCustomerAddressRequest.class)))
                .thenThrow(new BadRequestException("Address could not be located. Please provide a more specific address."));

        SaveAddressResult result = tool.saveDeliveryAddress(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Address could not be located");
    }

    @Test
    @DisplayName("saveDeliveryAddress handles maximum address limit error safely")
    void saveDeliveryAddress_maxAddressLimitReached_returnsSafeErrorResult() {
        SaveAddressInput input = new SaveAddressInput("Baluwatar, Kathmandu", "Home 11");

        when(customerAddressService.createAddress(eq(customerId), any(CreateCustomerAddressRequest.class)))
                .thenThrow(new BadRequestException("You can save up to 10 addresses."));

        SaveAddressResult result = tool.saveDeliveryAddress(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("You can save up to 10 addresses.");
    }

    @Test
    @DisplayName("saveDeliveryAddress handles generic exception without exposing internal details")
    void saveDeliveryAddress_genericError_returnsSafeMessage() {
        SaveAddressInput input = new SaveAddressInput("Thamel, Kathmandu", "Home");

        when(customerAddressService.createAddress(eq(customerId), any(CreateCustomerAddressRequest.class)))
                .thenThrow(new RuntimeException("Database connection timeout or internal failure"));

        SaveAddressResult result = tool.saveDeliveryAddress(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unable to save delivery address at this time. Please try again.");
    }
}

