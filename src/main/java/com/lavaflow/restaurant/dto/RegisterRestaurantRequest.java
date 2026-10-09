package com.lavaflow.restaurant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 150)
    private String restaurantName;

    @Size(max = 2000)
    private String restaurantDescription;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid restaurant phone number")
    private String restaurantPhone;

    @Email(message = "Invalid restaurant email format")
    @Size(max = 255)
    private String restaurantEmail;

    @NotBlank(message = "Admin full name is required")
    @Size(max = 150)
    private String adminFullName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email format")
    @Size(max = 255)
    private String adminEmail;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid admin phone number")
    private String adminPhone;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String adminPassword;
}