package com.shophelper.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 购物车项响应
 */
@Data
@AllArgsConstructor
public class CartItemResponse {

    private String itemId;

    private String productId;

    private String productName;

    private String productImage;

    private String skuId;

    private Map<String, Object> skuSpec;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalPrice;

    private Integer stock;

    private boolean selected;

    @JsonProperty("isAvailable")
    private boolean available;
}
