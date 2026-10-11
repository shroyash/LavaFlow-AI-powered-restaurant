package com.lavaflow.outlet.dto;

import com.lavaflow.common.enums.OutletStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOutletStatusRequest {

    @NotNull(message = "Status is required")
    private OutletStatus status;
}