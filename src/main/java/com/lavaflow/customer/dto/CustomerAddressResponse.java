package com.lavaflow.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerAddressResponse {

    private String id;
    private String label;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean defaultAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}