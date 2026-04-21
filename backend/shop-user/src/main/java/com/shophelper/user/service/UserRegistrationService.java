package com.shophelper.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.util.PhoneSecurityUtils;
import com.shophelper.user.config.UserSecurityProperties;
import com.shophelper.user.dto.RegisterUserRequest;
import com.shophelper.user.entity.UserEntity;
import com.shophelper.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户注册服务
 */
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityProperties userSecurityProperties;

    public void register(RegisterUserRequest request) {
        String nickname = request.getNickname().trim();
        String phoneHash = hashPhone(request.getPhone());

        ensureNicknameAvailable(nickname);
        ensurePhoneAvailable(phoneHash);

        UserEntity user = new UserEntity();
        user.setUsername(nickname);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneHash(phoneHash);
        user.setPhoneCiphertext(PhoneSecurityUtils.encryptToBase64(
                request.getPhone(),
                userSecurityProperties.getPhoneEncryptSecret()
        ));
        user.setStatus(1);
        user.setDeleteVersion(0L);
        user.setIsDeleted(0);

        userMapper.insert(user);
    }

    private void ensureNicknameAvailable(String nickname) {
        Long count = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, nickname)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称已存在，请更换后重试");
        }
    }

    private void ensurePhoneAvailable(String phoneHash) {
        Long count = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getPhoneHash, phoneHash)
                .eq(UserEntity::getDeleteVersion, 0L)
                .eq(UserEntity::getIsDeleted, 0));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号已注册");
        }
    }

    private String hashPhone(String phone) {
        try {
            return PhoneSecurityUtils.hmacSha256Hex(phone, userSecurityProperties.getPhoneHashSecret());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, e.getMessage());
        }
    }
}
