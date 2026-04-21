package com.shophelper.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
