package com.lavaflow.kitchen.config;

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

    private long timeoutMinutes = 30;
    private int maxConnectionsPerRestaurant = 20;
}
