package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.CustomerAddressListResult;
import com.lavaflow.ai.tool.dto.CustomerAddressToolResponse;
import com.lavaflow.ai.tool.dto.SaveAddressInput;
import com.lavaflow.ai.tool.dto.SaveAddressResult;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.service.CustomerAddressService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CustomerAddressTool {

    private static final int MAX_ADDRESS_LINE_LENGTH = 500;
    private static final int MAX_LABEL_LENGTH = 50;

    private final CustomerAddressService customerAddressService;

    public CustomerAddressTool(CustomerAddressService customerAddressService) {
        this.customerAddressService = customerAddressService;
    }

    @Tool(description = """
            List the authenticated customer's saved delivery addresses.
            Use this when the customer asks about their saved addresses or wants to select an existing delivery location.
            """)
    public CustomerAddressListResult listSavedAddresses(ToolContext toolContext) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return CustomerAddressListResult.error("Customer authentication context is missing.");
        }

        List<CustomerAddressToolResponse> addresses = customerAddressService.getAddresses(customerId)
                .stream()
                .map(addr -> new CustomerAddressToolResponse(
                        UUID.fromString(addr.getId()),
                        addr.getLabel(),
                        addr.getAddressLine(),
                        addr.isDefaultAddress()
                ))
                .toList();

        return CustomerAddressListResult.success(addresses);
    }

    @Tool(description = """
            Save a new delivery address for the authenticated customer.
            The address will be automatically geocoded and verified server-side.
            Use this when the customer explicitly provides an address they want saved for delivery.
            """)
    public SaveAddressResult saveDeliveryAddress(
            SaveAddressInput input,
            ToolContext toolContext
    ) {
        UUID customerId = extractCustomerId(toolContext);
        if (customerId == null) {
            return SaveAddressResult.error("Customer authentication context is missing.");
        }

        if (input == null || input.addressLine() == null || input.addressLine().isBlank()) {
            return SaveAddressResult.validationError("Address line must not be empty.");
        }

        String trimmedAddress = input.addressLine().trim();
        if (trimmedAddress.length() > MAX_ADDRESS_LINE_LENGTH) {
            return SaveAddressResult.validationError(
                    "Address line exceeds the maximum allowed length of " + MAX_ADDRESS_LINE_LENGTH + " characters."
            );
        }

        String trimmedLabel = null;
        if (input.label() != null && !input.label().isBlank()) {
            trimmedLabel = input.label().trim();
            if (trimmedLabel.length() > MAX_LABEL_LENGTH) {
                return SaveAddressResult.validationError(
                        "Address label exceeds the maximum allowed length of " + MAX_LABEL_LENGTH + " characters."
                );
            }
        }

        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest();
        request.setAddressLine(trimmedAddress);
        request.setLabel(trimmedLabel);
        // Ensure coordinates are not supplied by model; service resolves via authoritative geocoding
        request.setLatitude(null);
        request.setLongitude(null);

        try {
            CustomerAddressResponse created = customerAddressService.createAddress(customerId, request);
            return SaveAddressResult.success(new CustomerAddressToolResponse(
                    UUID.fromString(created.getId()),
                    created.getLabel(),
                    created.getAddressLine(),
                    created.isDefaultAddress()
            ));
        } catch (BadRequestException ex) {
            return SaveAddressResult.error(ex.getMessage());
        } catch (Exception ex) {
            return SaveAddressResult.error("Unable to save delivery address at this time. Please try again.");
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

