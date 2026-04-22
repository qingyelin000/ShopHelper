package com.shophelper.product.controller;

import com.shophelper.common.core.result.PageResult;
import com.shophelper.common.core.result.Result;
import com.shophelper.product.dto.ProductDetailResponse;
import com.shophelper.product.dto.ProductSummaryResponse;
import com.shophelper.product.service.ProductQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品查询接口
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping("/{productId}")
    public Result<ProductDetailResponse> getProduct(@PathVariable Long productId, HttpServletRequest request) {
        return Result.success(productQueryService.getProductDetail(productId))
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping
    public Result<PageResult<ProductSummaryResponse>> listProducts(
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {
        return Result.success(productQueryService.listProducts(categoryId, pageNum, pageSize))
                .requestId((String) request.getAttribute("requestId"));
    }
}
