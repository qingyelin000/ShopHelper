package com.shophelper.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 收货地址响应
 */
@Data
@AllArgsConstructor
public class UserAddressResponse {

    private String id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String postalCode;

    @JsonProperty("isDefault")
    private boolean defaultAddress;
}
