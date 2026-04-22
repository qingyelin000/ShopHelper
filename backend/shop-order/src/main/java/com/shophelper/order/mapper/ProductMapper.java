package com.shophelper.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.order.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
