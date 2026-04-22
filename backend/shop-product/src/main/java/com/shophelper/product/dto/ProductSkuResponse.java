package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品 SKU 响应
 */
@Data
@AllArgsConstructor
public class ProductSkuResponse {

    private String id;

    private String skuCode;

    private Map<String, Object> spec;

    private BigDecimal price;

    private Integer stock;

    private String status;
}
