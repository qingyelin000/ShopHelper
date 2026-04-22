package com.shophelper.common.core.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 供购物车/订单等内部服务消费的 SKU 快照
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuSnapshot {

    private Long skuId;

    private Long productId;

    private String productName;

    private String productImage;

    private String skuCode;

    private Map<String, Object> skuSpec;

    private BigDecimal unitPrice;

    private Integer stock;

    private String productStatus;

    private String skuStatus;

    @JsonProperty("isAvailable")
    private boolean available;
}
