package com.lavaflow.menu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMenuItemAvailabilityRequest {

    @NotNull(message = "Available flag is required")
    private Boolean available;
}