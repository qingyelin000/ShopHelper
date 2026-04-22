package com.shophelper.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单详情响应
 */
@Data
@AllArgsConstructor
public class OrderDetailResponse {

    private String orderId;

    private String orderNo;

    private String orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal payAmount;

    private String payType;

    private String source;

    private String remark;

    private Address address;

    private List<Item> items;

    private Object logistics;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    @Data
    @AllArgsConstructor
    public static class Address {

        private String receiverName;

        private String receiverPhone;

        private String fullAddress;
    }

    @Data
    @AllArgsConstructor
    public static class Item {

        private String itemId;

        private String productId;

        private String productName;

        private String productImage;

        private Map<String, Object> skuSpec;

        private BigDecimal unitPrice;

        private Integer quantity;

        private BigDecimal totalPrice;
    }
}
