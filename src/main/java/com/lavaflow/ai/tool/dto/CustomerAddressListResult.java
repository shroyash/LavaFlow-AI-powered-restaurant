package com.lavaflow.ai.tool.dto;

import java.util.List;

public record CustomerAddressListResult(
        boolean success,
        String message,
        List<CustomerAddressToolResponse> addresses
) {
    public static CustomerAddressListResult success(List<CustomerAddressToolResponse> addresses) {
        return new CustomerAddressListResult(true, "Addresses retrieved successfully.", addresses);
    }

    public static CustomerAddressListResult error(String message) {
        return new CustomerAddressListResult(false, message, List.of());
    }
}

