package com.shophelper.product.controller;

import com.shophelper.common.core.dto.product.ProductSkuSnapshot;
import com.shophelper.common.core.dto.product.QueryProductSkuSnapshotsRequest;
import com.shophelper.common.core.result.Result;
import com.shophelper.product.service.ProductQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品内部查询接口
 */
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductQueryService productQueryService;

    @GetMapping("/skus/{skuId}/snapshot")
    public Result<ProductSkuSnapshot> getSkuSnapshot(@PathVariable Long skuId) {
        return Result.success(productQueryService.getSkuSnapshot(skuId));
    }

    @PostMapping("/skus/snapshots/query")
    public Result<List<ProductSkuSnapshot>> querySkuSnapshots(@Valid @RequestBody QueryProductSkuSnapshotsRequest requestBody) {
        return Result.success(productQueryService.querySkuSnapshots(requestBody.getSkuIds()));
    }
}
