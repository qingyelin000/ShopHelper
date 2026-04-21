package com.shophelper.user.controller;

import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.Result;
import com.shophelper.user.dto.CreateUserAddressRequest;
import com.shophelper.user.dto.UpdateUserAddressRequest;
import com.shophelper.user.dto.UserAddressResponse;
import com.shophelper.user.service.UserAddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户收货地址接口
 */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public Result<List<UserAddressResponse>> list(HttpServletRequest request) {
        return Result.success(userAddressService.list(getCurrentUserId(request)))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping
    public Result<UserAddressResponse> create(@Valid @RequestBody CreateUserAddressRequest requestBody,
                                              HttpServletRequest request) {
        return Result.success(userAddressService.create(getCurrentUserId(request), requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/{addressId}")
    public Result<UserAddressResponse> update(@PathVariable Long addressId,
                                              @Valid @RequestBody UpdateUserAddressRequest requestBody,
                                              HttpServletRequest request) {
        return Result.success(userAddressService.update(getCurrentUserId(request), addressId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long addressId, HttpServletRequest request) {
        userAddressService.delete(getCurrentUserId(request), addressId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId, HttpServletRequest request) {
        userAddressService.setDefault(getCurrentUserId(request), addressId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return Long.valueOf(String.valueOf(request.getAttribute(CommonConstants.ATTR_CURRENT_USER_ID)));
    }
}
