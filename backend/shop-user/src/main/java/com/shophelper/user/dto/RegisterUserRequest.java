package com.shophelper.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterUserRequest {

    @NotBlank(message = "phone 不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    @NotBlank(message = "nickname 不能为空")
    @Size(min = 2, max = 32, message = "昵称长度需为 2-32 位")
    private String nickname;
}
