package com.lavaflow.ai.tool.dto;

public record SaveAddressResult(
        boolean success,
        String message,
        CustomerAddressToolResponse address
) {
    public static SaveAddressResult success(CustomerAddressToolResponse address) {
        return new SaveAddressResult(true, "Address saved successfully.", address);
    }

    public static SaveAddressResult error(String message) {
        return new SaveAddressResult(false, message, null);
    }

    public static SaveAddressResult validationError(String message) {
        return new SaveAddressResult(false, message, null);
    }
}

