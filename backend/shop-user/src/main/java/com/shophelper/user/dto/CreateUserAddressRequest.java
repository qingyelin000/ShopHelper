package com.shophelper.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增收货地址请求
 */
@Data
public class CreateUserAddressRequest {

    @NotBlank(message = "receiverName 不能为空")
    @Size(max = 64, message = "收货人姓名长度不能超过 64 位")
    private String receiverName;

    @NotBlank(message = "receiverPhone 不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的收货手机号")
    private String receiverPhone;

    @NotBlank(message = "province 不能为空")
    @Size(max = 32, message = "省份长度不能超过 32 位")
    private String province;

    @NotBlank(message = "city 不能为空")
    @Size(max = 32, message = "城市长度不能超过 32 位")
    private String city;

    @NotBlank(message = "district 不能为空")
    @Size(max = 32, message = "区县长度不能超过 32 位")
    private String district;

    @NotBlank(message = "detailAddress 不能为空")
    @Size(max = 256, message = "详细地址长度不能超过 256 位")
    private String detailAddress;

    @Size(max = 16, message = "邮编长度不能超过 16 位")
    private String postalCode;

    @JsonProperty("isDefault")
    private boolean defaultAddress;
}
