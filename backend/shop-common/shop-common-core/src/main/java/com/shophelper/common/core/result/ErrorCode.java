package com.shophelper.common.core.result;

/**
 * 业务错误码枚举
 */
public enum ErrorCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(40001, "参数校验失败"),
    UNAUTHORIZED(40101, "未登录或 Token 已失效"),
    FORBIDDEN(40301, "权限不足"),
    NOT_FOUND(40401, "资源不存在"),
    INTERNAL_ERROR(50001, "服务内部错误"),
    STOCK_INSUFFICIENT(50901, "库存不足"),
    SECKILL_ENDED(50902, "秒杀活动已结束"),
    SECKILL_ALREADY_JOINED(50903, "已参与过此次秒杀"),
    ORDER_STATUS_NOT_ALLOWED(50904, "订单状态不允许此操作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
