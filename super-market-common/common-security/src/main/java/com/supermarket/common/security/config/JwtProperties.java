package com.supermarket.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "super-market-default-secret-key-change-in-production-min-256-bits";
    private long accessTokenExpire = 7200;
    private long refreshTokenExpire = 604800;
    private String issuer = "super-market";
}
