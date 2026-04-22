package com.shophelper.order.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 订单服务用户安全配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shop.user")
public class OrderUserSecurityProperties {

    @NotBlank
    private String phoneEncryptSecret;
}
