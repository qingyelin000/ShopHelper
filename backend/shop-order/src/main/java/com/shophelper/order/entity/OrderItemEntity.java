package com.shophelper.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 */
@Data
@TableName("order_item")
public class OrderItemEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private Long productId;

    private String productName;

    private String productImage;

    private Long skuId;

    private String skuCode;

    private String skuSpecJson;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalPrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
