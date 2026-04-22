package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品摘要响应
 */
@Data
@AllArgsConstructor
public class ProductSummaryResponse {

    private String id;

    private String categoryId;

    private String name;

    private String mainImage;

    private BigDecimal price;

    private Integer salesCount;
}
