package com.shophelper.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 用户注册相关安全配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shop.user")
public class UserSecurityProperties {

    @NotBlank
    private String phoneHashSecret;

    @NotBlank
    private String phoneEncryptSecret;
}
