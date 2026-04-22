package com.shophelper.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
