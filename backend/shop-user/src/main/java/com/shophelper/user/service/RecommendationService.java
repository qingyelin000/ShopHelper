package com.shophelper.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.user.dto.RecommendationResponse;
import com.shophelper.user.entity.UserEntity;
import com.shophelper.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 当前用户推荐服务（冷启动版）
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Set<String> SUPPORTED_SCENES = Set.of(
            "homepage",
            "product_detail",
            "cart",
            "checkout",
            "category"
    );

    private final UserMapper userMapper;

    public RecommendationResponse getMyRecommendations(Long userId, String scene, Integer pageSize) {
        String normalizedScene = normalizeScene(scene);
        ensureUserActive(userId);
        int normalizedPageSize = normalizePageSize(pageSize);

        return new RecommendationResponse(
                normalizedScene,
                List.of(),
                0,
                normalizedPageSize > 0 ? "cold_start_v1" : "cold_start_v1"
        );
    }

    private void ensureUserActive(Long userId) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "scene 不能为空");
        }

        String normalized = scene.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SCENES.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "scene 仅支持 homepage / product_detail / cart / checkout / category"
            );
        }
        return normalized;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (pageSize < 1 || pageSize > CommonConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "pageSize 必须在 1-100 之间");
        }
        return pageSize;
    }
}
