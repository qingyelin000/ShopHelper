package com.shophelper.common.core.util;

import com.shophelper.common.core.enums.UserRole;

/**
 * JWT 中解析出的当前用户上下文
 */
public record JwtUserContext(Long userId, String username, UserRole role) {
}
