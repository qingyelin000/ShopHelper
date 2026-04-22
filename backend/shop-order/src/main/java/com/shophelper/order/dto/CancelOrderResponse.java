package com.shophelper.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 取消订单响应
 */
@Data
@AllArgsConstructor
public class CancelOrderResponse {

    private String orderId;

    private String orderNo;

    private String orderStatus;

    private LocalDateTime cancelTime;

    private String refundNote;
}
