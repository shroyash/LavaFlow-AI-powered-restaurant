package com.lavaflow.common.init;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lavaflow.bootstrap.super-admin")
public class SuperAdminProperties {

    private String email;
    private String password;
    private String fullName = "Super Admin";
}