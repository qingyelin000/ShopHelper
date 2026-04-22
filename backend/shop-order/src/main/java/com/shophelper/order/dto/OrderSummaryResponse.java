package com.shophelper.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项响应
 */
@Data
@AllArgsConstructor
public class OrderSummaryResponse {

    private String orderId;

    private String orderNo;

    private String orderStatus;

    private BigDecimal payAmount;

    private Integer itemCount;

    private String coverImage;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;
}
