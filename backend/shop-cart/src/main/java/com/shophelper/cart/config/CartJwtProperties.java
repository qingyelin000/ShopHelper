package com.shophelper.cart.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 购物车服务 JWT 校验配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shop.jwt")
public class CartJwtProperties {

    @NotBlank
    private String secret;
}
