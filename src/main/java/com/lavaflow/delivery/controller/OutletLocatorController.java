package com.lavaflow.delivery.controller;

import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.response.ApiResponse;
import com.lavaflow.delivery.dto.NearestOutletRequest;
import com.lavaflow.delivery.dto.NearestOutletResponse;
import com.lavaflow.delivery.service.OutletLocatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery coverage and outlet matching")
public class OutletLocatorController {

    private final OutletLocatorService outletLocatorService;

    @PostMapping("/nearest-outlet")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Find the nearest outlet that delivers to a location and has the requested items")
    public ResponseEntity<ApiResponse<NearestOutletResponse>> findNearestOutlet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NearestOutletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Nearest outlet found.",
                outletLocatorService.locate(principal.getUser().getId(), request)
        ));
    }
}