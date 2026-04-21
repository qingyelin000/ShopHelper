package com.shophelper.user.controller;

import com.shophelper.common.core.result.Result;
import com.shophelper.user.dto.RegisterUserRequest;
import com.shophelper.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest httpServletRequest) {
        userRegistrationService.register(request);
        return Result.<Void>success()
                .requestId((String) httpServletRequest.getAttribute("requestId"));
    }
}
