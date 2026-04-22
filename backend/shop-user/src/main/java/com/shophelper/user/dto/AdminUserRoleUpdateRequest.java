package com.shophelper.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员角色更新请求
 */
@Data
public class AdminUserRoleUpdateRequest {

    @NotBlank(message = "role 不能为空")
    private String role;
}
