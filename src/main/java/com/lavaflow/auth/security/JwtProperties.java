package com.lavaflow.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {

    private String privateKeyPath = "classpath:keys/private_key.pem";
    private String publicKeyPath  = "classpath:keys/public_key.pem";

    private Duration accessTokenExpiration  = Duration.ofMinutes(15);
    private Duration refreshTokenExpiration = Duration.ofDays(7);
}
