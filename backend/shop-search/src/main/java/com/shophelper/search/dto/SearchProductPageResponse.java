package com.shophelper.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 商品搜索分页响应
 */
@Data
@AllArgsConstructor
public class SearchProductPageResponse {

    private List<SearchProductResponse> list;

    private long total;

    private int pageNum;

    private int pageSize;

    private boolean hasNext;

    private SearchAggregationsResponse aggregations;
}
