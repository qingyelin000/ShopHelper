package com.shophelper.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.search.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
