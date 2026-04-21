package com.shophelper.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 当前用户推荐结果
 */
@Data
@AllArgsConstructor
public class RecommendationResponse {

    private String scene;
    private List<RecommendationItem> list;
    private int total;
    private String algorithm;

    @Data
    @AllArgsConstructor
    public static class RecommendationItem {
        private String productId;
        private String name;
        private String mainImage;
        private BigDecimal price;
        private int salesCount;
        private String recommendTag;
        private double score;
    }
}
