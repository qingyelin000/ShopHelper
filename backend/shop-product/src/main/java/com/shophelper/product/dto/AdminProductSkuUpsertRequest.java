package com.shophelper.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SKU 新增/更新请求
 */
@Data
public class AdminProductSkuUpsertRequest {

    @NotBlank(message = "skuCode 不能为空")
    @Size(max = 64, message = "skuCode 长度不能超过 64 位")
    private String skuCode;

    @NotEmpty(message = "spec 不能为空")
    private Map<String, Object> spec;

    @NotNull(message = "price 不能为空")
    @DecimalMin(value = "0.00", message = "price 不能小于 0")
    private BigDecimal price;

    @NotNull(message = "stock 不能为空")
    private Integer stock;

    @NotBlank(message = "status 不能为空")
    private String status;
}
