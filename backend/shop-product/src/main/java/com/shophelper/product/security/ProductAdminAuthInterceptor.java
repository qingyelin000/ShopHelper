package com.shophelper.product.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.enums.UserRole;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.Result;
import com.shophelper.common.core.util.JwtTokenUtils;
import com.shophelper.common.core.util.JwtUserContext;
import com.shophelper.product.config.ProductJwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 商品后台接口 JWT 二次校验
 */
@Component
@RequiredArgsConstructor
public class ProductAdminAuthInterceptor implements HandlerInterceptor {

    private final ProductJwtProperties productJwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            writeError(response, request, ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
            return false;
        }

        String accessToken = authorization.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        try {
            JwtUserContext userContext = JwtTokenUtils.parseAccessToken(accessToken, productJwtProperties.getSecret());
            if (userContext.role() != UserRole.ADMIN) {
                writeError(response, request, ErrorCode.FORBIDDEN, "权限不足");
                return false;
            }
            request.setAttribute(CommonConstants.ATTR_CURRENT_USER_ID, userContext.userId());
            request.setAttribute(CommonConstants.ATTR_CURRENT_USERNAME, userContext.username());
            request.setAttribute(CommonConstants.ATTR_CURRENT_USER_ROLE, userContext.role().name());
            return true;
        } catch (IllegalArgumentException e) {
            writeError(response, request, ErrorCode.UNAUTHORIZED, e.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response,
                            HttpServletRequest request,
                            ErrorCode errorCode,
                            String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(CommonConstants.HEADER_REQUEST_ID, String.valueOf(request.getAttribute("requestId")));
        Result<Void> result = Result.<Void>fail(errorCode, message)
                .requestId((String) request.getAttribute("requestId"));
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
