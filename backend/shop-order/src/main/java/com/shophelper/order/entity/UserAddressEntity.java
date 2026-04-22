package com.shophelper.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户地址实体（订单侧只读）
 */
@Data
@TableName("user_address")
public class UserAddressEntity {

    private Long id;

    private Long userId;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private String postalCode;

    private Integer isDefault;

    private Integer isDeleted;
}
