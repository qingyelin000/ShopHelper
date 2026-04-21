package com.shophelper.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 用户画像响应
 */
@Data
@AllArgsConstructor
public class UserProfileResponse {

    private String userId;
    private List<PreferredCategory> preferredCategories;
    private PriceBandPreference priceBandPreference;
    private int recentBrowseCount;
    private int purchaseCount;

    @Data
    @AllArgsConstructor
    public static class PreferredCategory {
        private String categoryId;
        private String categoryName;
        private double weight;
    }

    @Data
    @AllArgsConstructor
    public static class PriceBandPreference {
        private int min;
        private int max;
        private int typical;
    }
}
