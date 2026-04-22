package com.shophelper.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 首个管理员初始化请求
 */
@Data
public class AdminBootstrapRequest {

    @NotBlank(message = "username 不能为空")
    @Size(min = 2, max = 32, message = "用户名长度需为 2-32 位")
    private String username;
}
