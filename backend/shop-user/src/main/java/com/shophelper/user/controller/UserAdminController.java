package com.shophelper.user.controller;

import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.common.core.result.Result;
import com.shophelper.user.dto.AdminBootstrapRequest;
import com.shophelper.user.dto.AdminUserResponse;
import com.shophelper.user.dto.AdminUserRoleUpdateRequest;
import com.shophelper.user.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户后台管理与首个管理员初始化接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping("/bootstrap/admin")
    public Result<AdminUserResponse> bootstrapAdmin(@Valid @RequestBody AdminBootstrapRequest requestBody,
                                                    @RequestHeader(name = CommonConstants.HEADER_BOOTSTRAP_TOKEN, required = false)
                                                    String bootstrapToken,
                                                    HttpServletRequest request) {
        return Result.success(userAdminService.bootstrapAdmin(requestBody, bootstrapToken))
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping("/admin")
    public Result<PageResult<AdminUserResponse>> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {
        return Result.success(userAdminService.listUsers(keyword, role, status, pageNum, pageSize))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/admin/{userId}/role")
    public Result<AdminUserResponse> updateUserRole(@PathVariable Long userId,
                                                    @Valid @RequestBody AdminUserRoleUpdateRequest requestBody,
                                                    HttpServletRequest request) {
        return Result.success(userAdminService.updateUserRole(userId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }
}
