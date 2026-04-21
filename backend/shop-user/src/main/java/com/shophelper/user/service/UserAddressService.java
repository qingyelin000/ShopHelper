package com.shophelper.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.util.PhoneSecurityUtils;
import com.shophelper.user.config.UserSecurityProperties;
import com.shophelper.user.dto.CreateUserAddressRequest;
import com.shophelper.user.dto.UpdateUserAddressRequest;
import com.shophelper.user.dto.UserAddressResponse;
import com.shophelper.user.entity.UserAddressEntity;
import com.shophelper.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户收货地址服务
 */
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;
    private final UserSecurityProperties userSecurityProperties;

    public List<UserAddressResponse> list(Long userId) {
        return userAddressMapper.selectList(Wrappers.<UserAddressEntity>lambdaQuery()
                        .eq(UserAddressEntity::getUserId, userId)
                        .eq(UserAddressEntity::getIsDeleted, 0)
                        .orderByDesc(UserAddressEntity::getIsDefault)
                        .orderByDesc(UserAddressEntity::getUpdateTime)
                        .orderByDesc(UserAddressEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse create(Long userId, CreateUserAddressRequest request) {
        boolean shouldBeDefault = request.isDefaultAddress() || countActiveAddresses(userId) == 0;
        if (shouldBeDefault) {
            clearDefault(userId);
        }

        UserAddressEntity address = new UserAddressEntity();
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(encryptPhone(request.getReceiverPhone()));
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setPostalCode(normalizePostalCode(request.getPostalCode()));
        address.setIsDefault(shouldBeDefault ? 1 : 0);
        address.setIsDeleted(0);
        userAddressMapper.insert(address);
        return toResponse(address);
    }

    @Transactional
    public UserAddressResponse update(Long userId, Long addressId, UpdateUserAddressRequest request) {
        UserAddressEntity address = getAddress(userId, addressId);
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(encryptPhone(request.getReceiverPhone()));
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setPostalCode(normalizePostalCode(request.getPostalCode()));
        userAddressMapper.updateById(address);
        return toResponse(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        UserAddressEntity address = getAddress(userId, addressId);
        boolean wasDefault = Integer.valueOf(1).equals(address.getIsDefault());

        address.setIsDeleted(1);
        address.setIsDefault(0);
        userAddressMapper.updateById(address);

        if (wasDefault) {
            UserAddressEntity replacement = userAddressMapper.selectOne(Wrappers.<UserAddressEntity>lambdaQuery()
                    .eq(UserAddressEntity::getUserId, userId)
                    .eq(UserAddressEntity::getIsDeleted, 0)
                    .orderByDesc(UserAddressEntity::getUpdateTime)
                    .orderByDesc(UserAddressEntity::getId)
                    .last("LIMIT 1"));
            if (replacement != null) {
                replacement.setIsDefault(1);
                userAddressMapper.updateById(replacement);
            }
        }
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        UserAddressEntity address = getAddress(userId, addressId);
        clearDefault(userId);
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
    }

    private UserAddressEntity getAddress(Long userId, Long addressId) {
        UserAddressEntity address = userAddressMapper.selectOne(Wrappers.<UserAddressEntity>lambdaQuery()
                .eq(UserAddressEntity::getId, addressId)
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getIsDeleted, 0));
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private long countActiveAddresses(Long userId) {
        Long count = userAddressMapper.selectCount(Wrappers.<UserAddressEntity>lambdaQuery()
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getIsDeleted, 0));
        return count == null ? 0 : count;
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(
                null,
                Wrappers.<UserAddressEntity>lambdaUpdate()
                        .eq(UserAddressEntity::getUserId, userId)
                        .eq(UserAddressEntity::getIsDeleted, 0)
                        .eq(UserAddressEntity::getIsDefault, 1)
                        .set(UserAddressEntity::getIsDefault, 0)
        );
    }

    private String encryptPhone(String phone) {
        return PhoneSecurityUtils.encryptToBase64(phone, userSecurityProperties.getPhoneEncryptSecret());
    }

    private UserAddressResponse toResponse(UserAddressEntity address) {
        String decryptedPhone = PhoneSecurityUtils.decryptFromBase64(
                address.getReceiverPhone(),
                userSecurityProperties.getPhoneEncryptSecret()
        );
        return new UserAddressResponse(
                String.valueOf(address.getId()),
                address.getReceiverName(),
                PhoneSecurityUtils.maskChinaPhone(decryptedPhone),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getPostalCode(),
                Integer.valueOf(1).equals(address.getIsDefault())
        );
    }

    private String normalizePostalCode(String postalCode) {
        if (!StringUtils.hasText(postalCode)) {
            return null;
        }
        return postalCode.trim();
    }
}
