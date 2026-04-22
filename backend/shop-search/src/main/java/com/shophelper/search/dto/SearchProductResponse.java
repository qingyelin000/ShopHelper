package com.shophelper.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 搜索商品项响应
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchProductResponse {

    private String id;

    private String name;

    private String mainImage;

    private BigDecimal price;

    private Integer salesCount;

    private SearchHighlightResponse highlight;
}
