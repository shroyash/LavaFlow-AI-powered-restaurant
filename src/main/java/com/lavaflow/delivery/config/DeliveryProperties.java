package com.lavaflow.delivery.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lavaflow.delivery")
public class DeliveryProperties {

    private double defaultRadiusKm = 5.0;
    private double averageSpeedKmh = 20.0;
    private int prepMinutes = 15;
}