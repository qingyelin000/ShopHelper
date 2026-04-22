package com.shophelper.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车快照响应
 */
@Data
@AllArgsConstructor
public class CartSnapshotResponse {

    private List<CartItemResponse> items;

    private Integer selectedCount;

    private Integer totalQuantity;

    private BigDecimal estimatedTotal;
}
