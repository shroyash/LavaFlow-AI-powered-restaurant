package com.lavaflow.restaurant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String logoUrl;
}