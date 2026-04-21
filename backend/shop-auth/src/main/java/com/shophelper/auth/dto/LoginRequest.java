package com.shophelper.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "loginType 不能为空")
    private String loginType;

    @NotBlank(message = "principal 不能为空")
    private String principal;

    @NotBlank(message = "password 不能为空")
    private String password;
}
