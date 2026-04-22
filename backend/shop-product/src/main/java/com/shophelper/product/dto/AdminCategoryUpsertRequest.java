package com.shophelper.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/更新请求
 */
@Data
public class AdminCategoryUpsertRequest {

    private String parentId;

    @NotBlank(message = "name 不能为空")
    @Size(max = 64, message = "分类名称长度不能超过 64 位")
    private String name;

    @Size(max = 512, message = "iconUrl 长度不能超过 512 位")
    private String iconUrl;

    private Integer sortOrder;

    @NotBlank(message = "status 不能为空")
    private String status;
}
