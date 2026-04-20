package com.shophelper.common.core.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 请求 ID 头名称 */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** 幂等键头名称 */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    /** JWT 认证头名称 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** JWT 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 订单状态 */
    public static final int ORDER_STATUS_PENDING_PAYMENT = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_SHIPPED = 2;
    public static final int ORDER_STATUS_COMPLETED = 3;
    public static final int ORDER_STATUS_CANCELLED = 4;
    public static final int ORDER_STATUS_REFUNDING = 5;
    public static final int ORDER_STATUS_REFUNDED = 6;

    /** 分页默认值 */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
