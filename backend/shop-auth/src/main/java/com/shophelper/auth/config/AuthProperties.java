package com.shophelper.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 认证服务配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shop.auth")
public class AuthProperties {

    @NotBlank
    private String jwtSecret;

    @Min(60)
    private long accessTokenExpiresInSeconds = 7200;

    @Min(300)
    private long refreshTokenTtlSeconds = 604800;

    @NotBlank
    private String phoneHashSecret;
}
