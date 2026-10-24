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
import com.lavaflow.delivery.exception.OutletLocatorException;
import com.lavaflow.delivery.service.DeliveryLocationResolver;
import com.lavaflow.delivery.service.NearbyRestaurantService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NearbyRestaurantTool {

    private static final int MAX_ADDRESS_LINE_LENGTH = 500;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int DEFAULT_LIMIT = 10;

    private final NearbyRestaurantService nearbyRestaurantService;
    private final DeliveryLocationResolver deliveryLocationResolver;
    private final AddressGeocodingService addressGeocodingService;
    private final CustomerAddressService customerAddressService;

    public NearbyRestaurantTool(
            NearbyRestaurantService nearbyRestaurantService,
            DeliveryLocationResolver deliveryLocationResolver,
            AddressGeocodingService addressGeocodingService,
            CustomerAddressService customerAddressService
    ) {
        this.nearbyRestaurantService = nearbyRestaurantService;
        this.deliveryLocationResolver = deliveryLocationResolver;
        this.addressGeocodingService = addressGeocodingService;
        this.customerAddressService = customerAddressService;
    }

    @Tool(description = """
            Discover nearby restaurants and outlets deliverable to a specified address or the customer's default address.
            Supports filtering by restaurant or menu item query (e.g., 'pizza', 'burger').
            Accepts an addressId from saved addresses, a manualAddressLine for temporary/unsaved location searches, or omits both to use the default saved address.
            """)
    public NearbyRestaurantListResult findNearbyRestaurants(
            NearbyRestaurantSearchInput input,
            ToolContext toolContext
    ) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return NearbyRestaurantListResult.error("Customer authentication context is missing.");
        }

        DeliveryLocation location;
        if (input != null && input.addressId() != null) {
            try {
                location = deliveryLocationResolver.resolveAddress(customerId, input.addressId());
            } catch (EntityNotFoundException ex) {
                return NearbyRestaurantListResult.error("Address not found or does not belong to your account.");
            } catch (OutletLocatorException ex) {
                return NearbyRestaurantListResult.error(ex.getMessage());
            } catch (Exception ex) {
                return NearbyRestaurantListResult.error("Unable to resolve the specified address.");
            }
        } else if (input != null && input.manualAddressLine() != null && !input.manualAddressLine().isBlank()) {
            String trimmedAddress = input.manualAddressLine().trim();
            if (trimmedAddress.length() > MAX_ADDRESS_LINE_LENGTH) {
                return NearbyRestaurantListResult.validationError(
                        "Address line exceeds the maximum allowed length of " + MAX_ADDRESS_LINE_LENGTH + " characters."
                );
            }
            try {
                GeocodedAddress geocoded = addressGeocodingService.geocode(trimmedAddress);
                location = new DeliveryLocation(geocoded.latitude(), geocoded.longitude());
            } catch (BadRequestException ex) {
                return NearbyRestaurantListResult.validationError(ex.getMessage());
            } catch (Exception ex) {
                return NearbyRestaurantListResult.error("Unable to locate the provided address. Please provide a more specific address or try again.");
            }
        } else {
            try {
                List<CustomerAddressResponse> addresses = customerAddressService.getAddresses(customerId);
                var defaultAddrOpt = addresses.stream()
                        .filter(CustomerAddressResponse::isDefaultAddress)
                        .findFirst();

                if (defaultAddrOpt.isEmpty()) {
                    return NearbyRestaurantListResult.noAddressProvided(
                            "Please provide a delivery address or choose one of your saved addresses."
                    );
                }

                CustomerAddressResponse defaultAddr = defaultAddrOpt.get();
                if (defaultAddr.getLatitude() == null || defaultAddr.getLongitude() == null) {
                    return NearbyRestaurantListResult.error(
                            "Your default address has no map coordinates. Please provide a delivery address or update your address."
                    );
                }
                location = new DeliveryLocation(defaultAddr.getLatitude(), defaultAddr.getLongitude());
            } catch (Exception ex) {
                return NearbyRestaurantListResult.error("Unable to retrieve saved addresses. Please specify a delivery address.");
            }
        }

        String query = null;
        if (input != null && input.query() != null && !input.query().isBlank()) {
            query = input.query().trim();
            if (query.length() > MAX_QUERY_LENGTH) {
                return NearbyRestaurantListResult.validationError(
                        "Search query exceeds the maximum allowed length of " + MAX_QUERY_LENGTH + " characters."
                );
            }
        }

        try {
            List<NearbyRestaurantResponse> nearbyResponses = nearbyRestaurantService.findNearby(location, query, DEFAULT_LIMIT);
            List<NearbyRestaurantToolResponse> restaurants = nearbyResponses.stream()
                    .map(r -> new NearbyRestaurantToolResponse(
                            UUID.fromString(r.getRestaurantId()),
                            r.getRestaurantName(),
                            r.getDescription(),
                            UUID.fromString(r.getOutletId()),
                            r.getOutletName(),
                            r.getOutletAddress(),
                            r.getDistanceKm(),
                            r.getEstimatedDeliveryMinutes(),
                            r.isBusy()
                    ))
                    .toList();

            Object captureObject = toolContext.getContext().get("nearbyRestaurantResultCapture");

            if (captureObject instanceof NearbyRestaurantResultCapture capture) {
                capture.setRestaurants(restaurants);
            }

            return NearbyRestaurantListResult.success(restaurants);
        } catch (Exception ex) {
            return NearbyRestaurantListResult.error("Unable to search for nearby restaurants at this time. Please try again.");
        }
    }

    private UUID extractCustomerId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object idObj = toolContext.getContext().get("customerId");
        if (idObj instanceof UUID uuid) {
            return uuid;
        } else if (idObj instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}

