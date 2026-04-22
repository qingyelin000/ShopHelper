package com.shophelper.search.controller;

import com.shophelper.common.core.result.Result;
import com.shophelper.search.dto.SearchProductPageResponse;
import com.shophelper.search.service.ProductSearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品搜索接口
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/products")
    public Result<SearchProductPageResponse> searchProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "priceMin", required = false) String priceMin,
            @RequestParam(name = "priceMax", required = false) String priceMax,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortOrder", required = false) String sortOrder,
            HttpServletRequest request) {
        return Result.success(productSearchService.searchProducts(
                        keyword,
                        categoryId,
                        priceMin,
                        priceMax,
                        pageNum,
                        pageSize,
                        sortBy,
                        sortOrder))
                .requestId((String) request.getAttribute("requestId"));
    }
}
