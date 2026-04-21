package com.shophelper.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.user.dto.UserProfileResponse;
import com.shophelper.user.entity.UserEntity;
import com.shophelper.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户画像服务
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserMapper userMapper;

    public UserProfileResponse getCurrentUserProfile(HttpServletRequest request) {
        Object currentUserId = request.getAttribute(CommonConstants.ATTR_CURRENT_USER_ID);
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
        }

        Long userId = Long.valueOf(String.valueOf(currentUserId));
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

        return new UserProfileResponse(
                String.valueOf(user.getId()),
                List.of(),
                new UserProfileResponse.PriceBandPreference(0, 0, 0),
                0,
                0
        );
    }
}
