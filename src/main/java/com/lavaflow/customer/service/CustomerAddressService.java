
package com.lavaflow.customer.service;

import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.dto.GeocodedAddress;
import com.lavaflow.customer.dto.UpdateCustomerAddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final AddressGeocodingService addressGeocodingService;
    private final CustomerAddressPersistenceService persistenceService;

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAddresses(UUID customerId) {
        return persistenceService.getAddresses(customerId);
    }

    public CustomerAddressResponse createAddress(
            UUID customerId,
            CreateCustomerAddressRequest request
    ) {
        String addressLine = requireAddressLine(
                request.getAddressLine()
        );

        GeocodedAddress coordinates = resolveCoordinates(
                addressLine,
                request.getLatitude(),
                request.getLongitude()
        );

        return persistenceService.createAddress(
                customerId,
                request,
                addressLine,
                coordinates.latitude(),
                coordinates.longitude()
        );
    }

    public CustomerAddressResponse updateAddress(
            UUID customerId,
            UUID addressId,
            UpdateCustomerAddressRequest request
    ) {
        String addressLine = requireAddressLine(
                request.getAddressLine()
        );

        GeocodedAddress coordinates = resolveCoordinates(
                addressLine,
                request.getLatitude(),
                request.getLongitude()
        );

        return persistenceService.updateAddress(
                customerId,
                addressId,
                request,
                addressLine,
                coordinates.latitude(),
                coordinates.longitude()
        );
    }

    public void deleteAddress(
            UUID customerId,
            UUID addressId
    ) {
        persistenceService.deleteAddress(customerId, addressId);
    }

    private GeocodedAddress resolveCoordinates(
            String addressLine,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        requireCompleteCoordinates(latitude, longitude);

        if (latitude == null) {
            return addressGeocodingService.geocode(addressLine);
        }

        validateCoordinates(latitude, longitude);

        return new GeocodedAddress(
                latitude,
                longitude,
                addressLine
        );
    }

    private void validateCoordinates(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Invalid latitude.");
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Invalid longitude.");
        }
    }

    private void requireCompleteCoordinates(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException(
                    "Latitude and longitude must be provided together."
            );
        }
    }

    private String requireAddressLine(String addressLine) {
        if (addressLine == null || addressLine.isBlank()) {
            throw new BadRequestException(
                    "Delivery address must not be empty."
            );
        }

        return addressLine.trim();
    }
}
