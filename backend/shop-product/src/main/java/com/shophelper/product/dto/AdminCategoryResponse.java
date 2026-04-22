package com.shophelper.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类后台响应
 */
@Data
@AllArgsConstructor
public class AdminCategoryResponse {

    private String id;

    private String parentId;

    private String name;

    private String iconUrl;

    private Integer sortOrder;

    private Integer level;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
