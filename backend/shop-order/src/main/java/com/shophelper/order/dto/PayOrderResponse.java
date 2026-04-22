package com.shophelper.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发起支付响应
 */
@Data
@AllArgsConstructor
public class PayOrderResponse {

    private String orderId;

    private String orderNo;

    private String paymentMethod;

    private String paymentSessionId;

    private String payUrl;

    private String qrCode;

    private String note;
}
