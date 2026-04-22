package com.shophelper.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新购物车项请求
 */
@Data
public class UpdateCartItemRequest {

    @Min(value = 1, message = "quantity 必须大于 0")
    private Integer quantity;

    @JsonProperty("selected")
    private Boolean selected;
}
