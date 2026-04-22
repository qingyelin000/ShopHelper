package com.shophelper.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 搜索聚合响应
 */
@Data
@AllArgsConstructor
public class SearchAggregationsResponse {

    private List<SearchPriceRangeBucketResponse> priceRanges;

    private List<SearchCategoryBucketResponse> categories;
}
