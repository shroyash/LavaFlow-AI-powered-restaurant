package com.lavaflow.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdateMenuItemRequest {

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank(message = "Item name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @Size(max = 500)
    @Pattern(regexp = "^https?://\\S+$", message = "Image URL must start with http:// or https://")
    private String imageUrl;

    @NotNull(message = "Available flag is required")
    private Boolean available;
}