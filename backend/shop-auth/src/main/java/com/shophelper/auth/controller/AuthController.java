package com.shophelper.auth.controller;

import com.shophelper.auth.dto.LoginRequest;
import com.shophelper.auth.dto.RefreshTokenRequest;
import com.shophelper.auth.dto.TokenResponse;
import com.shophelper.auth.service.AuthService;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return Result.success(authService.login(request))
                .requestId(getRequestId(httpServletRequest));
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                         HttpServletRequest httpServletRequest) {
        return Result.success(authService.refresh(request))
                .requestId(getRequestId(httpServletRequest));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(name = CommonConstants.HEADER_AUTHORIZATION, required = false) String authorization,
                               HttpServletRequest httpServletRequest) {
        authService.logout(authorization);
        return Result.<Void>success()
                .requestId(getRequestId(httpServletRequest));
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }
}
