package com.shophelper.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.search.dto.SearchAggregationsResponse;
import com.shophelper.search.dto.SearchCategoryBucketResponse;
import com.shophelper.search.dto.SearchHighlightResponse;
import com.shophelper.search.dto.SearchPriceRangeBucketResponse;
import com.shophelper.search.dto.SearchProductPageResponse;
import com.shophelper.search.dto.SearchProductResponse;
import com.shophelper.search.entity.CategoryEntity;
import com.shophelper.search.entity.ProductEntity;
import com.shophelper.search.mapper.CategoryMapper;
import com.shophelper.search.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 商品搜索服务
 */
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final BigDecimal PRICE_RANGE_100 = new BigDecimal("100");
    private static final BigDecimal PRICE_RANGE_300 = new BigDecimal("300");
    private static final Set<String> SUPPORTED_SORT_BY = Set.of("default", "price", "sales", "new");
    private static final Set<String> SUPPORTED_SORT_ORDER = Set.of("asc", "desc");

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public SearchProductPageResponse searchProducts(String keyword,
                                                    String categoryId,
                                                    String priceMin,
                                                    String priceMax,
                                                    Integer pageNum,
                                                    Integer pageSize,
                                                    String sortBy,
                                                    String sortOrder) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Long normalizedCategoryId = normalizeOptionalLong(categoryId, "categoryId");
        BigDecimal normalizedPriceMin = normalizeOptionalPrice(priceMin, "priceMin");
        BigDecimal normalizedPriceMax = normalizeOptionalPrice(priceMax, "priceMax");
        validatePriceRange(normalizedPriceMin, normalizedPriceMax);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedSortBy = normalizeSortBy(sortBy);
        String normalizedSortOrder = normalizeSortOrder(sortOrder);
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        Long total = productMapper.selectCount(buildBaseQuery(normalizedKeyword, normalizedCategoryId, normalizedPriceMin, normalizedPriceMax));
        if (total == null || total == 0) {
            return new SearchProductPageResponse(
                    List.of(),
                    0,
                    normalizedPageNum,
                    normalizedPageSize,
                    false,
                    new SearchAggregationsResponse(defaultPriceRanges(0, 0, 0), List.of())
            );
        }

        List<SearchProductResponse> list = productMapper.selectList(buildListQuery(
                        normalizedKeyword,
                        normalizedCategoryId,
                        normalizedPriceMin,
                        normalizedPriceMax,
                        normalizedSortBy,
                        normalizedSortOrder
                ).last("LIMIT " + offset + ", " + normalizedPageSize))
                .stream()
                .map(product -> toSearchProductResponse(product, normalizedKeyword))
                .toList();

        List<ProductEntity> matchedProducts = productMapper.selectList(buildAggregationQuery(
                normalizedKeyword,
                normalizedCategoryId,
                normalizedPriceMin,
                normalizedPriceMax
        ));

        return new SearchProductPageResponse(
                list,
                total,
                normalizedPageNum,
                normalizedPageSize,
                (long) normalizedPageNum * normalizedPageSize < total,
                buildAggregations(matchedProducts)
        );
    }

    private LambdaQueryWrapper<ProductEntity> buildBaseQuery(String keyword,
                                                             Long categoryId,
                                                             BigDecimal priceMin,
                                                             BigDecimal priceMax) {
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getIsDeleted, 0)
                .eq(ProductEntity::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProductEntity::getName, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(ProductEntity::getCategoryId, categoryId);
        }
        if (priceMin != null) {
            wrapper.ge(ProductEntity::getPrice, priceMin);
        }
        if (priceMax != null) {
            wrapper.le(ProductEntity::getPrice, priceMax);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<ProductEntity> buildListQuery(String keyword,
                                                             Long categoryId,
                                                             BigDecimal priceMin,
                                                             BigDecimal priceMax,
                                                             String sortBy,
                                                             String sortOrder) {
        LambdaQueryWrapper<ProductEntity> wrapper = buildBaseQuery(keyword, categoryId, priceMin, priceMax)
                .select(
                        ProductEntity::getId,
                        ProductEntity::getName,
                        ProductEntity::getMainImage,
                        ProductEntity::getPrice,
                        ProductEntity::getSalesCount,
                        ProductEntity::getCreateTime,
                        ProductEntity::getUpdateTime
                );

        boolean asc = "asc".equals(sortOrder);
        switch (sortBy) {
            case "price" -> wrapper.orderBy(true, asc, ProductEntity::getPrice);
            case "sales", "default" -> wrapper.orderBy(true, asc, ProductEntity::getSalesCount);
            case "new" -> wrapper.orderBy(true, asc, ProductEntity::getCreateTime);
            default -> wrapper.orderByDesc(ProductEntity::getSalesCount);
        }
        return wrapper.orderByDesc(ProductEntity::getUpdateTime)
                .orderByDesc(ProductEntity::getId);
    }

    private LambdaQueryWrapper<ProductEntity> buildAggregationQuery(String keyword,
                                                                    Long categoryId,
                                                                    BigDecimal priceMin,
                                                                    BigDecimal priceMax) {
        return buildBaseQuery(keyword, categoryId, priceMin, priceMax)
                .select(ProductEntity::getId, ProductEntity::getCategoryId, ProductEntity::getPrice);
    }

    private SearchProductResponse toSearchProductResponse(ProductEntity product, String keyword) {
        return new SearchProductResponse(
                String.valueOf(product.getId()),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getSalesCount(),
                buildHighlight(product.getName(), keyword)
        );
    }

    private SearchHighlightResponse buildHighlight(String name, String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(name)) {
            return null;
        }
        String highlighted = name.replaceAll("(?i)" + Pattern.quote(keyword), "<em>$0</em>");
        if (highlighted.equals(name)) {
            return null;
        }
        return new SearchHighlightResponse(highlighted);
    }

    private SearchAggregationsResponse buildAggregations(List<ProductEntity> matchedProducts) {
        long range0To100 = matchedProducts.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(PRICE_RANGE_100) <= 0)
                .count();
        long range100To300 = matchedProducts.stream()
                .filter(product -> product.getPrice() != null
                        && product.getPrice().compareTo(PRICE_RANGE_100) > 0
                        && product.getPrice().compareTo(PRICE_RANGE_300) <= 0)
                .count();
        long range300Plus = matchedProducts.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(PRICE_RANGE_300) > 0)
                .count();

        Map<Long, Long> categoryCountMap = matchedProducts.stream()
                .filter(product -> product.getCategoryId() != null)
                .collect(Collectors.groupingBy(ProductEntity::getCategoryId, Collectors.counting()));

        Map<Long, String> categoryNameMap = categoryCountMap.isEmpty()
                ? Map.of()
                : categoryMapper.selectBatchIds(categoryCountMap.keySet()).stream()
                .filter(category -> category.getIsDeleted() != null && category.getIsDeleted() == 0)
                .filter(category -> category.getStatus() == null || category.getStatus() == 1)
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        List<SearchCategoryBucketResponse> categories = categoryCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new SearchCategoryBucketResponse(
                        String.valueOf(entry.getKey()),
                        categoryNameMap.getOrDefault(entry.getKey(), String.valueOf(entry.getKey())),
                        entry.getValue()))
                .toList();

        return new SearchAggregationsResponse(
                defaultPriceRanges(range0To100, range100To300, range300Plus),
                categories
        );
    }

    private List<SearchPriceRangeBucketResponse> defaultPriceRanges(long range0To100, long range100To300, long range300Plus) {
        return List.of(
                new SearchPriceRangeBucketResponse("0-100", range0To100),
                new SearchPriceRangeBucketResponse("100-300", range100To300),
                new SearchPriceRangeBucketResponse("300+", range300Plus)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private Long normalizeOptionalLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 必须大于 0");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 格式非法");
        }
    }

    private BigDecimal normalizeOptionalPrice(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 不能小于 0");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 格式非法");
        }
    }

    private void validatePriceRange(BigDecimal priceMin, BigDecimal priceMax) {
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "priceMin 不能大于 priceMax");
        }
    }

    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null) {
            return CommonConstants.DEFAULT_PAGE_NUM;
        }
        if (pageNum < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "pageNum 必须大于等于 1");
        }
        return pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > CommonConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "pageSize 必须在 1-100 之间");
        }
        return pageSize;
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "default";
        }
        String normalized = sortBy.trim().toLowerCase();
        if (!SUPPORTED_SORT_BY.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "sortBy 仅支持 default / price / sales / new");
        }
        return normalized;
    }

    private String normalizeSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "desc";
        }
        String normalized = sortOrder.trim().toLowerCase();
        if (!SUPPORTED_SORT_ORDER.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "sortOrder 仅支持 asc / desc");
        }
        return normalized;
    }
}
