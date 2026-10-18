package com.lavaflow.customer.service;

import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.dto.UpdateCustomerAddressRequest;
import com.lavaflow.customer.entity.CustomerAddress;
import com.lavaflow.customer.mapper.CustomerAddressMapper;
import com.lavaflow.customer.repository.CustomerAddressRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private static final int MAX_ADDRESSES = 10;

    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;
    private final CustomerAddressMapper customerAddressMapper;

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAddresses(UUID customerId) {
        return customerAddressRepository
                .findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId)
                .stream()
                .map(customerAddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse createAddress(UUID customerId, CreateCustomerAddressRequest request) {
        requireCompleteCoordinates(request.getLatitude(), request.getLongitude());

        long existingCount = customerAddressRepository.countByCustomerId(customerId);
        if (existingCount >= MAX_ADDRESSES) {
            throw new BadRequestException("You can save up to " + MAX_ADDRESSES + " addresses.");
        }

        CustomerAddress address = customerAddressMapper.toEntity(request);
        address.setCustomer(userRepository.getReferenceById(customerId));
        address.setLabel(normalizeLabel(request.getLabel()));
        address.setAddressLine(request.getAddressLine().trim());

        boolean makeDefault = existingCount == 0 || Boolean.TRUE.equals(request.getDefaultAddress());
        if (makeDefault) {
            customerAddressRepository.clearDefaults(customerId, Instant.now());
        }
        address.setDefaultAddress(makeDefault);

        return customerAddressMapper.toResponse(customerAddressRepository.saveAndFlush(address));
    }

    @Transactional
    public CustomerAddressResponse updateAddress(
            UUID customerId,
            UUID addressId,
            UpdateCustomerAddressRequest request
    ) {
        requireCompleteCoordinates(request.getLatitude(), request.getLongitude());

        CustomerAddress address = findOwned(customerId, addressId);

        customerAddressMapper.updateEntity(request, address);
        address.setLabel(normalizeLabel(request.getLabel()));
        address.setAddressLine(request.getAddressLine().trim());

        if (Boolean.TRUE.equals(request.getDefaultAddress()) && !address.isDefaultAddress()) {
            customerAddressRepository.clearDefaults(customerId, Instant.now());
            address.setDefaultAddress(true);
        }

        return customerAddressMapper.toResponse(customerAddressRepository.saveAndFlush(address));
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = findOwned(customerId, addressId);
        boolean wasDefault = address.isDefaultAddress();

        customerAddressRepository.delete(address);
        customerAddressRepository.flush();

        if (wasDefault) {
            customerAddressRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId)
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        customerAddressRepository.save(next);
                    });
        }
    }

    private CustomerAddress findOwned(UUID customerId, UUID addressId) {
        return customerAddressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found."));
    }

    private void requireCompleteCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Latitude and longitude must be provided together.");
        }
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return label.trim();
    }
}
