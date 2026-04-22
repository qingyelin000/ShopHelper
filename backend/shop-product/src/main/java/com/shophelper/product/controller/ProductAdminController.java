package com.shophelper.product.controller;

import com.shophelper.common.core.result.PageResult;
import com.shophelper.common.core.result.Result;
import com.shophelper.product.dto.AdminCategoryResponse;
import com.shophelper.product.dto.AdminCategoryUpsertRequest;
import com.shophelper.product.dto.AdminProductDetailResponse;
import com.shophelper.product.dto.AdminProductSkuResponse;
import com.shophelper.product.dto.AdminProductSkuUpsertRequest;
import com.shophelper.product.dto.AdminProductSummaryResponse;
import com.shophelper.product.dto.AdminProductUpsertRequest;
import com.shophelper.product.service.ProductAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品后台管理接口
 */
@RestController
@RequestMapping("/api/v1/products/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    @GetMapping("/categories")
    public Result<List<AdminCategoryResponse>> listCategories(HttpServletRequest request) {
        return Result.success(productAdminService.listCategories())
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping("/categories/{categoryId}")
    public Result<AdminCategoryResponse> getCategory(@PathVariable Long categoryId, HttpServletRequest request) {
        return Result.success(productAdminService.getCategory(categoryId))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/categories")
    public Result<AdminCategoryResponse> createCategory(@Valid @RequestBody AdminCategoryUpsertRequest requestBody,
                                                        HttpServletRequest request) {
        return Result.success(productAdminService.createCategory(requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/categories/{categoryId}")
    public Result<AdminCategoryResponse> updateCategory(@PathVariable Long categoryId,
                                                        @Valid @RequestBody AdminCategoryUpsertRequest requestBody,
                                                        HttpServletRequest request) {
        return Result.success(productAdminService.updateCategory(categoryId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping("/categories/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId, HttpServletRequest request) {
        productAdminService.deleteCategory(categoryId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping
    public Result<PageResult<AdminProductSummaryResponse>> listProducts(
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {
        return Result.success(productAdminService.listProducts(categoryId, status, keyword, pageNum, pageSize))
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping("/{productId}")
    public Result<AdminProductDetailResponse> getProduct(@PathVariable Long productId, HttpServletRequest request) {
        return Result.success(productAdminService.getProduct(productId))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping
    public Result<AdminProductDetailResponse> createProduct(@Valid @RequestBody AdminProductUpsertRequest requestBody,
                                                            HttpServletRequest request) {
        return Result.success(productAdminService.createProduct(requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/{productId}")
    public Result<AdminProductDetailResponse> updateProduct(@PathVariable Long productId,
                                                            @Valid @RequestBody AdminProductUpsertRequest requestBody,
                                                            HttpServletRequest request) {
        return Result.success(productAdminService.updateProduct(productId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable Long productId, HttpServletRequest request) {
        productAdminService.deleteProduct(productId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/{productId}/skus")
    public Result<AdminProductSkuResponse> createSku(@PathVariable Long productId,
                                                     @Valid @RequestBody AdminProductSkuUpsertRequest requestBody,
                                                     HttpServletRequest request) {
        return Result.success(productAdminService.createSku(productId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/{productId}/skus/{skuId}")
    public Result<AdminProductSkuResponse> updateSku(@PathVariable Long productId,
                                                     @PathVariable Long skuId,
                                                     @Valid @RequestBody AdminProductSkuUpsertRequest requestBody,
                                                     HttpServletRequest request) {
        return Result.success(productAdminService.updateSku(productId, skuId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping("/{productId}/skus/{skuId}")
    public Result<Void> deleteSku(@PathVariable Long productId,
                                  @PathVariable Long skuId,
                                  HttpServletRequest request) {
        productAdminService.deleteSku(productId, skuId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }
}
