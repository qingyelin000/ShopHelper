package com.shophelper.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体
 */
@Data
@TableName("`order`")
public class OrderEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long addressId;

    private String snapReceiverName;

    private String snapReceiverPhone;

    private String snapProvince;

    private String snapCity;

    private String snapDistrict;

    private String snapDetailAddress;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal freightAmount;

    private Integer payType;

    private String source;

    private Integer status;

    private String cancelReason;

    private String remark;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
