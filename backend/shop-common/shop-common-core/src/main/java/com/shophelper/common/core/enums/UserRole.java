package com.shophelper.common.core.enums;

import java.util.Locale;

/**
 * 用户角色
 */
public enum UserRole {
    USER,
    ADMIN;

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("角色信息缺失");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("角色信息非法", ex);
        }
    }
}
