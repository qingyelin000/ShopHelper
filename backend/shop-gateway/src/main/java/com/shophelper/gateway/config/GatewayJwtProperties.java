package com.shophelper.gateway.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 网关 JWT 校验配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shop.jwt")
public class GatewayJwtProperties {

    @NotBlank
    private String secret;
}
