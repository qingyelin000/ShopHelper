package com.shophelper.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索服务内的商品实体
 */
@Data
@TableName("product")
public class ProductEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String name;

    private String mainImage;

    private BigDecimal price;

    private Integer status;

    private Integer salesCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
