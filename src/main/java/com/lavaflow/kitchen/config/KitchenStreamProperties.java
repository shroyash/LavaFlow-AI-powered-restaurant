package com.lavaflow.kitchen.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "lavaflow.kitchen.stream")
public class KitchenStreamProperties {

    @NotBlank
    @Size(min = 32)
    private String ticketSecret;

    private long ticketTtlSeconds = 60;
    private long timeoutMinutes = 30;
    private int maxConnectionsPerRestaurant = 20;
}
