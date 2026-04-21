package com.shophelper.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.Result;
import com.shophelper.common.core.util.JwtTokenUtils;
import com.shophelper.common.core.util.JwtUserContext;
import com.shophelper.gateway.config.GatewayJwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 网关统一 JWT 鉴权过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayJwtProperties gatewayJwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!requiresAuthentication(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return writeUnauthorized(exchange, "未登录或 Token 已失效");
        }

        String accessToken = authorization.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        try {
            JwtUserContext userContext = JwtTokenUtils.parseAccessToken(accessToken, gatewayJwtProperties.getSecret());
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(CommonConstants.HEADER_USER_ID, String.valueOf(userContext.userId()))
                    .header(CommonConstants.HEADER_USERNAME, userContext.username())
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (IllegalArgumentException e) {
            return writeUnauthorized(exchange, e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean requiresAuthentication(String path) {
        return "/api/v1/auth/logout".equals(path)
                || path.startsWith("/api/v1/users/") && !"/api/v1/users/register".equals(path)
                || path.startsWith("/api/v1/recommendations")
                || path.startsWith("/api/v1/cart")
                || path.startsWith("/api/v1/orders")
                || path.startsWith("/api/v1/seckill")
                || path.startsWith("/api/v1/agent");
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        String requestId = exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }

        Result<Void> result = Result.<Void>fail(ErrorCode.UNAUTHORIZED, message)
                .requestId(requestId);

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            body = ("{\"code\":40101,\"message\":\"" + message + "\",\"requestId\":\"" + requestId + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(CommonConstants.HEADER_REQUEST_ID, requestId);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
