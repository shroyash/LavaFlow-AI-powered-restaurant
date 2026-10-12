package com.lavaflow.customer.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.customer.dto.CreateCustomerAddressRequest;
import com.lavaflow.customer.dto.CustomerAddressResponse;
import com.lavaflow.customer.dto.UpdateCustomerAddressRequest;
import com.lavaflow.customer.service.CustomerAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Customer Addresses", description = "Saved delivery addresses of the logged-in customer")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    @PostMapping
    @Operation(summary = "Save a new address")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCustomerAddressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Address saved.",
                customerAddressService.createAddress(principal.getUser().getId(), request)
        ));
    }

    @GetMapping
    @Operation(summary = "List my addresses, default first")
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Addresses fetched.",
                customerAddressService.getAddresses(principal.getUser().getId())
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerAddressRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Address updated.",
                customerAddressService.updateAddress(principal.getUser().getId(), id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        customerAddressService.deleteAddress(principal.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted."));
    }
}