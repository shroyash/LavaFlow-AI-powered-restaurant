package com.lavaflow.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerProfileResponse {

    private String userId;
    private String email;
    private boolean emailVerified;
    private String fullName;
    private String phone;
    private LocalDateTime createdAt;
    private CustomerAddressResponse defaultAddress;
}
