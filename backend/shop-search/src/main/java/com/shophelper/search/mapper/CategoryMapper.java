package com.shophelper.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.search.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索分类 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
}
