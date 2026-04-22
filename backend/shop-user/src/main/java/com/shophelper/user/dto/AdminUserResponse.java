package com.shophelper.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户响应
 */
@Data
@AllArgsConstructor
public class AdminUserResponse {

    private String userId;
    private String username;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
