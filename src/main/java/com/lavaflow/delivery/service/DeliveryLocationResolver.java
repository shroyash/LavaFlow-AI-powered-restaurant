package com.lavaflow.delivery.service;

import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.entity.CustomerAddress;
import com.lavaflow.customer.repository.CustomerAddressRepository;
import com.lavaflow.delivery.dto.DeliveryLocation;
import com.lavaflow.delivery.enums.LocatorFailureReason;
import com.lavaflow.delivery.exception.OutletLocatorException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryLocationResolver {

    private final CustomerAddressRepository customerAddressRepository;

    public DeliveryLocation resolve(UUID customerId, UUID addressId, BigDecimal latitude, BigDecimal longitude) {
        boolean hasAddress = addressId != null;
        boolean hasCoordinates = latitude != null && longitude != null;

        if (hasAddress == hasCoordinates) {
            throw new BadRequestException("Provide either an address id or both latitude and longitude.");
        }

        return hasAddress
                ? resolveAddress(customerId, addressId)
                : new DeliveryLocation(latitude, longitude);
    }

    public DeliveryLocation resolveAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found."));

        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new OutletLocatorException(
                    LocatorFailureReason.ADDRESS_NEEDS_LOCATION,
                    "This address has no map location. Please share your location or pick the spot on the map."
            );
        }

        return new DeliveryLocation(address.getLatitude(), address.getLongitude());
    }
}