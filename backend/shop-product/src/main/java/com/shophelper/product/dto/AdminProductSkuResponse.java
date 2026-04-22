package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SKU 后台响应
 */
@Data
@AllArgsConstructor
public class AdminProductSkuResponse {

    private String id;

    private String productId;

    private String skuCode;

    private Map<String, Object> spec;

    private String specHash;

    private BigDecimal price;

    private Integer stock;

    private Integer version;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
