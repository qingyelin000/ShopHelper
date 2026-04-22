package com.shophelper.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单响应
 */
@Data
@AllArgsConstructor
public class CreateOrderResponse {

    private String orderId;

    private String orderNo;

    private String orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal freightAmount;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
