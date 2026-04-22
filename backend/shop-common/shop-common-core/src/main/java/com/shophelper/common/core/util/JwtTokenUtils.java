package com.shophelper.common.core.util;

import com.shophelper.common.core.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 解析工具
 */
public final class JwtTokenUtils {

    private JwtTokenUtils() {
    }

    public static SecretKey buildSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static JwtUserContext parseAccessToken(String accessToken, String secret) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(buildSigningKey(secret))
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            String tokenType = claims.get("tokenType", String.class);
            String subject = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            if (!"access".equals(tokenType) || subject == null || username == null || username.isBlank()) {
                throw new IllegalArgumentException("Token 缺少有效用户信息");
            }

            return new JwtUserContext(Long.parseLong(subject), username, UserRole.from(role));
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("未登录或 Token 已失效", e);
        }
    }
}
