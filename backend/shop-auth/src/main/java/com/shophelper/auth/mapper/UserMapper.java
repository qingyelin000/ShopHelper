package com.shophelper.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户查询 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
