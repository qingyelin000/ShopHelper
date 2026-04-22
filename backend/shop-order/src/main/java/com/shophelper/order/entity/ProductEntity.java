package com.shophelper.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品实体（订单侧只读）
 */
@Data
@TableName("product")
public class ProductEntity {

    private Long id;

    private Long categoryId;

    private String name;

    private String mainImage;

    private Integer status;

    private Integer isDeleted;
}
