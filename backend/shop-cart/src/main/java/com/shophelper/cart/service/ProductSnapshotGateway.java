package com.shophelper.cart.service;

import com.shophelper.cart.client.ProductClient;
import com.shophelper.common.core.dto.product.ProductSkuSnapshot;
import com.shophelper.common.core.dto.product.QueryProductSkuSnapshotsRequest;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品快照查询网关
 */
@Component
@RequiredArgsConstructor
public class ProductSnapshotGateway {

    private final ProductClient productClient;

    public ProductSkuSnapshot getSkuSnapshot(Long skuId) {
        try {
            Result<ProductSkuSnapshot> result = productClient.getSkuSnapshot(skuId);
            return unwrap(result);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务暂不可用");
        }
    }

    public List<ProductSkuSnapshot> querySkuSnapshots(List<Long> skuIds) {
        try {
            QueryProductSkuSnapshotsRequest request = new QueryProductSkuSnapshotsRequest();
            request.setSkuIds(skuIds);
            Result<List<ProductSkuSnapshot>> result = productClient.querySkuSnapshots(request);
            return unwrap(result);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务暂不可用");
        }
    }

    private <T> T unwrap(Result<T> result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务返回为空");
        }
        if (result.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new BusinessException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }
}
