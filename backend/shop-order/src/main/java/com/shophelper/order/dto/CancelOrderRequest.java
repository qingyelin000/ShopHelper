package com.shophelper.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消订单请求
 */
@Data
public class CancelOrderRequest {

    @NotBlank(message = "reason 不能为空")
    private String reason;
}
