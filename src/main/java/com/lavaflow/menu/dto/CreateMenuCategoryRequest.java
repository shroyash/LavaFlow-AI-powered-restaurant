package com.lavaflow.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMenuCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private Boolean active;
}