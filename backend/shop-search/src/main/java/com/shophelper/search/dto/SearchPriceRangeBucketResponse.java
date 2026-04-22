package com.shophelper.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 搜索价格区间聚合
 */
@Data
@AllArgsConstructor
public class SearchPriceRangeBucketResponse {

    private String label;

    private long count;
}
