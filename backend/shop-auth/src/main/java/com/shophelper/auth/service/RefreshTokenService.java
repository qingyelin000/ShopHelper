package com.shophelper.auth.service;

import com.shophelper.auth.config.AuthProperties;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的刷新令牌存储与轮换
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedissonClient redissonClient;
    private final AuthProperties authProperties;

    public String issue(Long userId) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = "rt_" + userId + "_" + tokenId;

        getBucket(buildKey(userId, tokenId)).set(
                refreshToken,
                authProperties.getRefreshTokenTtlSeconds(),
                TimeUnit.SECONDS
        );
        getTokenIdSet(userId).add(tokenId);
        getTokenIdSet(userId).expire(authProperties.getRefreshTokenTtlSeconds(), TimeUnit.SECONDS);
        return refreshToken;
    }

    public Long consume(String refreshToken) {
        RefreshTokenPayload payload = parse(refreshToken);
        RBucket<String> bucket = getBucket(buildKey(payload.userId(), payload.tokenId()));
        String cachedToken = bucket.get();
        if (cachedToken == null || !cachedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token 已失效");
        }

        bucket.delete();
        getTokenIdSet(payload.userId()).remove(payload.tokenId());
        return payload.userId();
    }

    public void revokeAll(Long userId) {
        RSet<String> tokenIdSet = getTokenIdSet(userId);
        Set<String> tokenIds = tokenIdSet.readAll();
        for (String tokenId : tokenIds) {
            getBucket(buildKey(userId, tokenId)).delete();
        }
        tokenIdSet.delete();
    }

    private RBucket<String> getBucket(String key) {
        return redissonClient.getBucket(key, StringCodec.INSTANCE);
    }

    private RSet<String> getTokenIdSet(Long userId) {
        return redissonClient.getSet(buildUserIndexKey(userId), StringCodec.INSTANCE);
    }

    private String buildKey(Long userId, String tokenId) {
        return "auth:refresh:" + userId + ":" + tokenId;
    }

    private String buildUserIndexKey(Long userId) {
        return "auth:refresh:index:" + userId;
    }

    private RefreshTokenPayload parse(String refreshToken) {
        String[] parts = refreshToken.split("_", 3);
        if (parts.length != 3 || !"rt".equals(parts[0])) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token 已失效");
        }

        try {
            Long userId = Long.parseLong(parts[1]);
            return new RefreshTokenPayload(userId, parts[2]);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token 已失效");
        }
    }

    private record RefreshTokenPayload(Long userId, String tokenId) {
    }
}
