package com.shophelper.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
