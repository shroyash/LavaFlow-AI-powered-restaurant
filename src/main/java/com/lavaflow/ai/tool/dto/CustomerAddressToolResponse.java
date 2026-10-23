package com.lavaflow.ai.tool.dto;

import java.util.UUID;

public record CustomerAddressToolResponse(
        UUID addressId,
        String label,
        String addressLine,
        boolean defaultAddress
) {
}

