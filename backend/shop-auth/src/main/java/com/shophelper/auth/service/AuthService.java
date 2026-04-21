package com.shophelper.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.auth.config.AuthProperties;
import com.shophelper.auth.dto.LoginRequest;
import com.shophelper.auth.dto.RefreshTokenRequest;
import com.shophelper.auth.dto.TokenResponse;
import com.shophelper.auth.entity.UserEntity;
import com.shophelper.auth.mapper.UserMapper;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.util.PhoneSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;

    public TokenResponse login(LoginRequest request) {
        LoginType loginType = LoginType.from(request.getLoginType());
        UserEntity user = findUser(loginType, request.getPrincipal());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号或密码错误");
        }
        ensureUserActive(user);
        return issueTokenResponse(user);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        Long userId = refreshTokenService.consume(request.getRefreshToken());
        UserEntity user = getActiveUserById(userId);
        return issueTokenResponse(user);
    }

    public void logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
        }

        String accessToken = authorization.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        Long userId = jwtTokenService.parseUserIdFromAccessToken(accessToken);
        refreshTokenService.revokeAll(userId);
    }

    private UserEntity findUser(LoginType loginType, String principal) {
        return switch (loginType) {
            case PHONE -> userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getPhoneHash, hashPhone(principal))
                    .eq(UserEntity::getDeleteVersion, 0L)
                    .eq(UserEntity::getIsDeleted, 0));
            case EMAIL -> userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getEmail, principal.trim().toLowerCase(Locale.ROOT))
                    .eq(UserEntity::getDeleteVersion, 0L)
                    .eq(UserEntity::getIsDeleted, 0));
            case USERNAME -> userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getUsername, principal.trim())
                    .eq(UserEntity::getDeleteVersion, 0L)
                    .eq(UserEntity::getIsDeleted, 0));
        };
    }

    private UserEntity getActiveUserById(Long userId) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token 已失效");
        }
        ensureUserActive(user);
        return user;
    }

    private void ensureUserActive(UserEntity user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    private TokenResponse issueTokenResponse(UserEntity user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new TokenResponse(
                accessToken,
                refreshToken,
                authProperties.getAccessTokenExpiresInSeconds(),
                TOKEN_TYPE_BEARER
        );
    }

    private String hashPhone(String rawPhone) {
        try {
            return PhoneSecurityUtils.hmacSha256Hex(rawPhone, authProperties.getPhoneHashSecret());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    private enum LoginType {
        PHONE,
        EMAIL,
        USERNAME;

        private static LoginType from(String value) {
            if (!StringUtils.hasText(value)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "loginType 不能为空");
            }

            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "phone" -> PHONE;
                case "email" -> EMAIL;
                case "username" -> USERNAME;
                default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "loginType 仅支持 phone / email / username");
            };
        }
    }
}
