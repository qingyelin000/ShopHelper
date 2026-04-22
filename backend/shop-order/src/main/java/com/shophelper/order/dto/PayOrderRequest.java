package com.shophelper.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起支付请求
 */
@Data
public class PayOrderRequest {

    @NotBlank(message = "paymentMethod 不能为空")
    private String paymentMethod;
}
