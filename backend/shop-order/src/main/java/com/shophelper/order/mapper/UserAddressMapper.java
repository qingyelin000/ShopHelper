package com.shophelper.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.order.entity.UserAddressEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户地址 Mapper
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddressEntity> {
}
