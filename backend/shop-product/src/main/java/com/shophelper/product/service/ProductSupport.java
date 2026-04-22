package com.shophelper.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商品模块通用转换与状态工具
 */
@Component
@RequiredArgsConstructor
public class ProductSupport {

    private final ObjectMapper objectMapper;

    public Map<String, Object> parseSpecJson(String specJson) {
        if (!StringUtils.hasText(specJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(specJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SKU 规格数据格式非法");
        }
    }

    public String toSpecJson(Map<String, Object> spec) {
        if (spec == null || spec.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "spec 不能为空");
        }
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(spec));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "spec 序列化失败");
        }
    }

    public String buildSpecHash(Map<String, Object> spec) {
        return DigestUtils.md5DigestAsHex(toSpecJson(spec).getBytes(StandardCharsets.UTF_8));
    }

    public String mapProductStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING_REVIEW";
            case 1 -> "ON_SALE";
            case 2 -> "OFF_SALE";
            default -> "UNKNOWN";
        };
    }

    public Integer parseProductStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 不能为空");
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING_REVIEW" -> 0;
            case "ON_SALE" -> 1;
            case "OFF_SALE" -> 2;
            default -> throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "status 仅支持 PENDING_REVIEW / ON_SALE / OFF_SALE"
            );
        };
    }

    public String mapSkuStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "DISABLED";
            case 1 -> "ENABLED";
            default -> "UNKNOWN";
        };
    }

    public Integer parseSkuStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 不能为空");
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "DISABLED" -> 0;
            case "ENABLED" -> 1;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "status 仅支持 ENABLED / DISABLED");
        };
    }

    public Integer parseCategoryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 不能为空");
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ENABLED" -> 1;
            case "DISABLED" -> 0;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "status 仅支持 ENABLED / DISABLED");
        };
    }

    public String mapCategoryStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 1 -> "ENABLED";
            case 0 -> "DISABLED";
            default -> "UNKNOWN";
        };
    }
}
