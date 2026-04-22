package com.shophelper.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 搜索服务内的分类实体
 */
@Data
@TableName("category")
public class CategoryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Integer status;

    private Integer isDeleted;
}
