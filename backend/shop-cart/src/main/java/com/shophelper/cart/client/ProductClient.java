package com.shophelper.cart.client;

import com.shophelper.common.core.dto.product.ProductSkuSnapshot;
import com.shophelper.common.core.dto.product.QueryProductSkuSnapshotsRequest;
import com.shophelper.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商品服务内部查询客户端
 */
@FeignClient(name = "shop-product")
public interface ProductClient {

    @GetMapping("/internal/products/skus/{skuId}/snapshot")
    Result<ProductSkuSnapshot> getSkuSnapshot(@PathVariable("skuId") Long skuId);

    @PostMapping("/internal/products/skus/snapshots/query")
    Result<List<ProductSkuSnapshot>> querySkuSnapshots(@RequestBody QueryProductSkuSnapshotsRequest request);
}
