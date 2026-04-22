package com.shophelper.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品新增/更新请求
 */
@Data
public class AdminProductUpsertRequest {

    @NotBlank(message = "categoryId 不能为空")
    private String categoryId;

    @NotBlank(message = "name 不能为空")
    @Size(max = 256, message = "商品名称长度不能超过 256 位")
    private String name;

    @Size(max = 512, message = "subTitle 长度不能超过 512 位")
    private String subTitle;

    @Size(max = 512, message = "mainImage 长度不能超过 512 位")
    private String mainImage;

    private String description;

    @NotBlank(message = "status 不能为空")
    private String status;
}
