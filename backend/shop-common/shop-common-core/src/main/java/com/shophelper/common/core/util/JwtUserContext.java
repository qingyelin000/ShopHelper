package com.shophelper.common.core.util;

/**
 * JWT 中解析出的当前用户上下文
 */
public record JwtUserContext(Long userId, String username) {
}
