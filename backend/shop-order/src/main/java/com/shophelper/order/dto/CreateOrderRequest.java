package com.shophelper.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    @NotBlank(message = "addressId 不能为空")
    private String addressId;

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<Item> items;

    private String remark;

    private String source;

    @Data
    public static class Item {

        @NotBlank(message = "skuId 不能为空")
        private String skuId;

        @NotNull(message = "quantity 不能为空")
        private Integer quantity;
    }
}
