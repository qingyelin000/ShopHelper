package com.shophelper.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 搜索分类聚合
 */
@Data
@AllArgsConstructor
public class SearchCategoryBucketResponse {

    private String id;

    private String name;

    private long count;
}
