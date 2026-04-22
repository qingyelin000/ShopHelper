package com.shophelper.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 实体（订单侧查询/扣减库存）
 */
@Data
@TableName("product_sku")
public class ProductSkuEntity {

    private Long id;

    private Long productId;

    private String skuCode;

    private String specJson;

    private BigDecimal price;

    private Integer stock;

    private Integer version;

    private Integer status;

    private Integer isDeleted;
}
