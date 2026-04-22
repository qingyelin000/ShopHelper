package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品后台列表响应
 */
@Data
@AllArgsConstructor
public class AdminProductSummaryResponse {

    private String id;

    private String categoryId;

    private String categoryName;

    private String name;

    private String mainImage;

    private BigDecimal price;

    private Integer salesCount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
