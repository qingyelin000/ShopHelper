package com.shophelper.auth.service;

import com.shophelper.auth.config.AuthProperties;
import com.shophelper.auth.entity.UserEntity;
import com.shophelper.common.core.enums.UserRole;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.util.JwtTokenUtils;
import com.shophelper.common.core.util.JwtUserContext;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发服务
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final AuthProperties authProperties;

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getAccessTokenExpiresInSeconds());
        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : UserRole.USER.name();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", role)
                .claim("tokenType", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public Long parseUserIdFromAccessToken(String accessToken) {
        try {
            JwtUserContext userContext = JwtTokenUtils.parseAccessToken(accessToken, authProperties.getJwtSecret());
            return userContext.userId();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
        }
    }

    private SecretKey getSigningKey() {
        return JwtTokenUtils.buildSigningKey(authProperties.getJwtSecret());
    }
}
