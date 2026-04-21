package com.shophelper.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录/刷新返回
 */
@Data
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
}
