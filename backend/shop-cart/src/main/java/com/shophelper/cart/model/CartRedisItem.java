package com.shophelper.cart.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Redis 中的购物车项
 */
@Data
public class CartRedisItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String itemId;

    private String productId;

    private String productName;

    private String productImage;

    private String skuId;

    private String skuCode;

    private Map<String, Object> skuSpec;

    private BigDecimal unitPrice;

    private Integer quantity;

    private Integer stock;

    private String productStatus;

    private String skuStatus;

    private boolean selected;

    private boolean available;

    private Long updatedAt;
}
