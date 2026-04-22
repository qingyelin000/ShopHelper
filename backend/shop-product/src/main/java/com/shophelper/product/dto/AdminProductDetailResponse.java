package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品后台详情响应
 */
@Data
@AllArgsConstructor
public class AdminProductDetailResponse {

    private String id;

    private String categoryId;

    private String categoryName;

    private String name;

    private String subTitle;

    private String mainImage;

    private String description;

    private BigDecimal price;

    private Integer salesCount;

    private String status;

    private List<AdminProductSkuResponse> skuList;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
