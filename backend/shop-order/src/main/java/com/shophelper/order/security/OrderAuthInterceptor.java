package com.shophelper.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.Result;
import com.shophelper.common.core.util.JwtTokenUtils;
import com.shophelper.common.core.util.JwtUserContext;
import com.shophelper.order.config.OrderJwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 订单服务 JWT 二次校验
 */
@Component
@RequiredArgsConstructor
public class OrderAuthInterceptor implements HandlerInterceptor {

    private final OrderJwtProperties orderJwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            writeUnauthorized(response, request, "未登录或 Token 已失效");
            return false;
        }

        String accessToken = authorization.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        try {
            JwtUserContext userContext = JwtTokenUtils.parseAccessToken(accessToken, orderJwtProperties.getSecret());
            request.setAttribute(CommonConstants.ATTR_CURRENT_USER_ID, userContext.userId());
            request.setAttribute(CommonConstants.ATTR_CURRENT_USERNAME, userContext.username());
            request.setAttribute(CommonConstants.ATTR_CURRENT_USER_ROLE, userContext.role().name());
            return true;
        } catch (IllegalArgumentException e) {
            writeUnauthorized(response, request, e.getMessage());
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(CommonConstants.HEADER_REQUEST_ID, String.valueOf(request.getAttribute("requestId")));
        Result<Void> result = Result.<Void>fail(ErrorCode.UNAUTHORIZED, message)
                .requestId((String) request.getAttribute("requestId"));
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
