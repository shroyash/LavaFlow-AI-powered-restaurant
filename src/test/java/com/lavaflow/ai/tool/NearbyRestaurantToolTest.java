package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.NearbyRestaurantListResult;
import com.lavaflow.ai.tool.dto.NearbyRestaurantSearchInput;
import com.lavaflow.ai.tool.dto.NearbyRestaurantToolResponse;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.dto.GeocodedAddress;
import com.lavaflow.customer.service.AddressGeocodingService;
import com.lavaflow.customer.service.CustomerAddressService;
import com.lavaflow.delivery.dto.DeliveryLocation;
import com.lavaflow.delivery.dto.NearbyRestaurantResponse;
import com.lavaflow.delivery.enums.LocatorFailureReason;
import com.lavaflow.delivery.exception.OutletLocatorException;
import com.lavaflow.delivery.service.DeliveryLocationResolver;
import com.lavaflow.delivery.service.NearbyRestaurantService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NearbyRestaurantTool Unit Tests")
class NearbyRestaurantToolTest {

    @Mock
    private NearbyRestaurantService nearbyRestaurantService;

    @Mock
    private DeliveryLocationResolver deliveryLocationResolver;

    @Mock
    private AddressGeocodingService addressGeocodingService;

    @Mock
    private CustomerAddressService customerAddressService;

    private NearbyRestaurantTool tool;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        tool = new NearbyRestaurantTool(
                nearbyRestaurantService,
                deliveryLocationResolver,
                addressGeocodingService,
                customerAddressService
        );
        customerId = UUID.randomUUID();
    }

    private ToolContext createContext(UUID custId) {
        if (custId == null) {
            return new ToolContext(Map.of());
        }
        return new ToolContext(Map.of("customerId", custId));
    }

    @Test
    @DisplayName("findNearbyRestaurants fails closed when customerId is missing")
    void findNearbyRestaurants_missingCustomerId_failsClosed() {
        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, null, "pizza");
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(null));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.restaurants()).isEmpty();
        verifyNoInteractions(nearbyRestaurantService);
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants fails closed when customerId in ToolContext is malformed string")
    void findNearbyRestaurants_malformedCustomerIdString_failsClosed() {
        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, null, "pizza");
        ToolContext malformedContext = new ToolContext(Map.of("customerId", "not-a-valid-uuid"));
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, malformedContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.restaurants()).isEmpty();
        verifyNoInteractions(nearbyRestaurantService);
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants fails closed when customerId in ToolContext is unexpected type")
    void findNearbyRestaurants_unexpectedCustomerIdType_failsClosed() {
        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, null, "pizza");
        ToolContext unexpectedTypeContext = new ToolContext(Map.of("customerId", 99999));
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, unexpectedTypeContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Customer authentication context is missing");
        assertThat(result.restaurants()).isEmpty();
        verifyNoInteractions(nearbyRestaurantService);
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants resolves addressId through DeliveryLocationResolver")
    void findNearbyRestaurants_withSavedAddressId_success() {
        UUID addressId = UUID.randomUUID();
        DeliveryLocation resolvedLocation = new DeliveryLocation(new BigDecimal("27.7172"), new BigDecimal("85.3240"));
        when(deliveryLocationResolver.resolveAddress(customerId, addressId)).thenReturn(resolvedLocation);

        UUID restaurantId = UUID.randomUUID();
        UUID outletId = UUID.randomUUID();
        NearbyRestaurantResponse response = NearbyRestaurantResponse.builder()
                .restaurantId(restaurantId.toString())
                .restaurantName("Himalayan Java")
                .description("Specialty coffee & bakery")
                .outletId(outletId.toString())
                .outletName("Civil Mall Outlet")
                .outletAddress("Sundhara, Kathmandu")
                .distanceKm(new BigDecimal("1.5"))
                .estimatedDeliveryMinutes(25)
                .busy(false)
                .build();

        when(nearbyRestaurantService.findNearby(resolvedLocation, "coffee", 10))
                .thenReturn(List.of(response));

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(addressId, null, "coffee");
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isTrue();
        assertThat(result.restaurants()).hasSize(1);
        NearbyRestaurantToolResponse res = result.restaurants().get(0);
        assertThat(res.restaurantId()).isEqualTo(restaurantId);
        assertThat(res.restaurantName()).isEqualTo("Himalayan Java");
        assertThat(res.outletId()).isEqualTo(outletId);
        assertThat(res.distanceKm()).isEqualByComparingTo("1.5");
        assertThat(res.estimatedDeliveryMinutes()).isEqualTo(25);
        assertThat(res.busy()).isFalse();

        verify(deliveryLocationResolver).resolveAddress(customerId, addressId);
        verify(nearbyRestaurantService).findNearby(resolvedLocation, "coffee", 10);
        verifyNoInteractions(addressGeocodingService);
    }

    @Test
    @DisplayName("findNearbyRestaurants handles EntityNotFoundException on unowned addressId")
    void findNearbyRestaurants_unownedAddress_returnsError() {
        UUID addressId = UUID.randomUUID();
        when(deliveryLocationResolver.resolveAddress(customerId, addressId))
                .thenThrow(new EntityNotFoundException("Address not found."));

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(addressId, null, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Address not found or does not belong to your account");
        verifyNoInteractions(nearbyRestaurantService);
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants handles OutletLocatorException on address without location")
    void findNearbyRestaurants_addressNeedsLocation_returnsError() {
        UUID addressId = UUID.randomUUID();
        when(deliveryLocationResolver.resolveAddress(customerId, addressId))
                .thenThrow(new OutletLocatorException(
                        LocatorFailureReason.ADDRESS_NEEDS_LOCATION,
                        "This address has no map location. Please share your location or pick the spot on the map."
                ));

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(addressId, null, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("This address has no map location");
        verifyNoInteractions(nearbyRestaurantService);
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants geocodes temporary manualAddressLine without saving it")
    void findNearbyRestaurants_manualAddress_geocodesAndSearches() {
        String manualAddress = "Baluwatar, Kathmandu";
        GeocodedAddress geocoded = new GeocodedAddress(new BigDecimal("27.7250"), new BigDecimal("85.3300"), "Baluwatar, Kathmandu, Nepal");
        when(addressGeocodingService.geocode(manualAddress)).thenReturn(geocoded);

        DeliveryLocation expectedLocation = new DeliveryLocation(geocoded.latitude(), geocoded.longitude());
        when(nearbyRestaurantService.findNearby(expectedLocation, null, 10))
                .thenReturn(List.of());

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, manualAddress, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isTrue();
        assertThat(result.restaurants()).isEmpty();

        verify(addressGeocodingService).geocode(manualAddress);
        verify(nearbyRestaurantService).findNearby(expectedLocation, null, 10);
        // Ensure manual address is NOT persisted to customerAddressService
        verifyNoInteractions(customerAddressService);
    }

    @Test
    @DisplayName("findNearbyRestaurants validates oversized manualAddressLine")
    void findNearbyRestaurants_oversizedAddress_rejected() {
        String longAddress = "X".repeat(501);
        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, longAddress, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("exceeds the maximum allowed length");
        verifyNoInteractions(addressGeocodingService);
        verifyNoInteractions(nearbyRestaurantService);
    }

    @Test
    @DisplayName("findNearbyRestaurants handles geocoding BadRequestException cleanly")
    void findNearbyRestaurants_geocodingError_returnsCleanMessage() {
        String invalidAddress = "Nowhere XYZ 999";
        when(addressGeocodingService.geocode(invalidAddress))
                .thenThrow(new BadRequestException("Address could not be located. Please provide a more specific address."));

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, invalidAddress, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Address could not be located");
        verifyNoInteractions(nearbyRestaurantService);
    }

    @Test
    @DisplayName("findNearbyRestaurants falls back to default saved address when no address supplied")
    void findNearbyRestaurants_usesDefaultAddress() {
        CustomerAddressResponse defaultAddress = CustomerAddressResponse.builder()
                .id(UUID.randomUUID().toString())
                .label("Home")
                .addressLine("Jhamsikhel, Lalitpur")
                .defaultAddress(true)
                .latitude(new BigDecimal("27.6780"))
                .longitude(new BigDecimal("85.3120"))
                .build();

        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(defaultAddress));

        DeliveryLocation expectedLocation = new DeliveryLocation(defaultAddress.getLatitude(), defaultAddress.getLongitude());
        when(nearbyRestaurantService.findNearby(expectedLocation, "burger", 10)).thenReturn(List.of());

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, null, "burger");
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isTrue();
        verify(customerAddressService).getAddresses(customerId);
        verify(nearbyRestaurantService).findNearby(expectedLocation, "burger", 10);
    }

    @Test
    @DisplayName("findNearbyRestaurants returns noAddressProvided when no address provided and no default exists")
    void findNearbyRestaurants_noDefaultAddress_returnsNoAddressProvided() {
        CustomerAddressResponse nonDefaultAddress = CustomerAddressResponse.builder()
                .id(UUID.randomUUID().toString())
                .label("Gym")
                .addressLine("Kupondole, Lalitpur")
                .defaultAddress(false)
                .latitude(new BigDecimal("27.6850"))
                .longitude(new BigDecimal("85.3180"))
                .build();

        when(customerAddressService.getAddresses(customerId)).thenReturn(List.of(nonDefaultAddress));

        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(null, null, null);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Please provide a delivery address or choose one of your saved addresses");
        verifyNoInteractions(nearbyRestaurantService);
    }

    @Test
    @DisplayName("findNearbyRestaurants validates oversized query string")
    void findNearbyRestaurants_oversizedQuery_rejected() {
        UUID addressId = UUID.randomUUID();
        when(deliveryLocationResolver.resolveAddress(customerId, addressId))
                .thenReturn(new DeliveryLocation(new BigDecimal("27.7"), new BigDecimal("85.3")));

        String longQuery = "Q".repeat(101);
        NearbyRestaurantSearchInput input = new NearbyRestaurantSearchInput(addressId, null, longQuery);
        NearbyRestaurantListResult result = tool.findNearbyRestaurants(input, createContext(customerId));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Search query exceeds the maximum allowed length");
        verifyNoInteractions(nearbyRestaurantService);
    }
}

