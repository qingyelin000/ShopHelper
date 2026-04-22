package com.shophelper.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入购物车请求
 */
@Data
public class AddCartItemRequest {

    @NotBlank(message = "skuId 不能为空")
    private String skuId;

    @NotNull(message = "quantity 不能为空")
    @Min(value = 1, message = "quantity 必须大于 0")
    private Integer quantity;
}
