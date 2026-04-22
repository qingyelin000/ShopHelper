package com.shophelper.common.core.dto.product;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量查询 SKU 快照请求
 */
@Data
public class QueryProductSkuSnapshotsRequest {

    @NotEmpty(message = "skuIds 不能为空")
    private List<Long> skuIds;
}
