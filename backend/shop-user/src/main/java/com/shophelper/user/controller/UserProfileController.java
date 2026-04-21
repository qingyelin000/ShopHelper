package com.shophelper.user.controller;

import com.shophelper.common.core.result.Result;
import com.shophelper.user.dto.UserProfileResponse;
import com.shophelper.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户画像接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me/profile")
    public Result<UserProfileResponse> getMyProfile(HttpServletRequest request) {
        return Result.success(userProfileService.getCurrentUserProfile(request))
                .requestId((String) request.getAttribute("requestId"));
    }
}
