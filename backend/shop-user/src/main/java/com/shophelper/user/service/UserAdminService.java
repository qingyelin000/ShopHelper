package com.shophelper.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.enums.UserRole;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.user.config.UserSecurityProperties;
import com.shophelper.user.dto.AdminBootstrapRequest;
import com.shophelper.user.dto.AdminUserResponse;
import com.shophelper.user.dto.AdminUserRoleUpdateRequest;
import com.shophelper.user.entity.UserEntity;
import com.shophelper.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户后台管理服务
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserMapper userMapper;
    private final UserSecurityProperties userSecurityProperties;

    @Transactional
    public AdminUserResponse bootstrapAdmin(AdminBootstrapRequest request, String bootstrapToken) {
        String configuredToken = userSecurityProperties.getAdminBootstrapToken();
        if (!StringUtils.hasText(configuredToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前环境未配置管理员初始化令牌");
        }
        if (!configuredToken.equals(bootstrapToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员初始化令牌不正确");
        }
        if (countEnabledAdmins() > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统中已存在管理员，不能再次初始化");
        }

        UserEntity user = getUserByUsername(request.getUsername().trim());
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "目标账号已被禁用，不能授予管理员");
        }
        user.setRole(UserRole.ADMIN.name());
        userMapper.updateById(user);
        return toAdminUserResponse(user);
    }

    public PageResult<AdminUserResponse> listUsers(String keyword,
                                                   String role,
                                                   Integer status,
                                                   Integer pageNum,
                                                   Integer pageSize) {
        UserRole normalizedRole = normalizeOptionalRole(role);
        Integer normalizedStatus = normalizeOptionalStatus(status);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        LambdaQueryWrapper<UserEntity> baseQuery = buildUserQuery(keyword, normalizedRole, normalizedStatus);
        Long total = userMapper.selectCount(baseQuery);
        if (total == null || total == 0) {
            return PageResult.of(List.of(), 0, normalizedPageNum, normalizedPageSize);
        }

        List<AdminUserResponse> list = userMapper.selectList(buildUserQuery(keyword, normalizedRole, normalizedStatus)
                        .orderByDesc(UserEntity::getUpdateTime)
                        .orderByDesc(UserEntity::getId)
                        .last("LIMIT " + offset + ", " + normalizedPageSize))
                .stream()
                .map(this::toAdminUserResponse)
                .toList();
        return PageResult.of(list, total, normalizedPageNum, normalizedPageSize);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long userId, AdminUserRoleUpdateRequest request) {
        UserEntity user = getUserById(userId);
        UserRole targetRole = UserRole.from(request.getRole());
        UserRole currentRole = StringUtils.hasText(user.getRole()) ? UserRole.from(user.getRole()) : UserRole.USER;
        if (currentRole == targetRole) {
            return toAdminUserResponse(user);
        }
        if (currentRole == UserRole.ADMIN
                && targetRole != UserRole.ADMIN
                && isEnabled(user)
                && countEnabledAdmins() <= 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统至少需要保留一个可用管理员");
        }

        user.setRole(targetRole.name());
        userMapper.updateById(user);
        return toAdminUserResponse(user);
    }

    private LambdaQueryWrapper<UserEntity> buildUserQuery(String keyword, UserRole role, Integer status) {
        LambdaQueryWrapper<UserEntity> query = Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            query.like(UserEntity::getUsername, keyword.trim());
        }
        if (role != null) {
            query.eq(UserEntity::getRole, role.name());
        }
        if (status != null) {
            query.eq(UserEntity::getStatus, status);
        }
        return query;
    }

    private UserEntity getUserById(Long userId) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserEntity getUserByUsername(String username) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }
        return user;
    }

    private long countEnabledAdmins() {
        Long count = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getRole, UserRole.ADMIN.name())
                .eq(UserEntity::getStatus, 1)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        return count == null ? 0 : count;
    }

    private boolean isEnabled(UserEntity user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    private UserRole normalizeOptionalRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        return UserRole.from(role);
    }

    private Integer normalizeOptionalStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 仅支持 0 或 1");
        }
        return status;
    }

    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null) {
            return CommonConstants.DEFAULT_PAGE_NUM;
        }
        if (pageNum < CommonConstants.DEFAULT_PAGE_NUM) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "pageNum 不能小于 1");
        }
        return pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > CommonConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "pageSize 必须在 1-" + CommonConstants.MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    private AdminUserResponse toAdminUserResponse(UserEntity user) {
        String role = StringUtils.hasText(user.getRole()) ? UserRole.from(user.getRole()).name() : UserRole.USER.name();
        return new AdminUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                role,
                user.getStatus(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }
}
