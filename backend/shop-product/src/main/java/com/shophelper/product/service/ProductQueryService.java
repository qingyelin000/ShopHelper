package com.shophelper.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.dto.product.ProductSkuSnapshot;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.product.dto.ProductDetailResponse;
import com.shophelper.product.dto.ProductSkuResponse;
import com.shophelper.product.dto.ProductSummaryResponse;
import com.shophelper.product.entity.CategoryEntity;
import com.shophelper.product.entity.ProductEntity;
import com.shophelper.product.entity.ProductSkuEntity;
import com.shophelper.product.mapper.CategoryMapper;
import com.shophelper.product.mapper.ProductMapper;
import com.shophelper.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品查询服务
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSupport productSupport;

    public ProductDetailResponse getProductDetail(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "productId 非法");
        }

        ProductEntity product = productMapper.selectOne(Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getId, productId)
                .eq(ProductEntity::getIsDeleted, 0)
                .eq(ProductEntity::getStatus, 1));
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }

        CategoryEntity category = categoryMapper.selectOne(Wrappers.<CategoryEntity>lambdaQuery()
                .eq(CategoryEntity::getId, product.getCategoryId())
                .eq(CategoryEntity::getIsDeleted, 0));

        List<ProductSkuResponse> skuList = productSkuMapper.selectList(Wrappers.<ProductSkuEntity>lambdaQuery()
                        .eq(ProductSkuEntity::getProductId, product.getId())
                        .eq(ProductSkuEntity::getIsDeleted, 0)
                        .eq(ProductSkuEntity::getStatus, 1)
                        .orderByAsc(ProductSkuEntity::getId))
                .stream()
                .map(this::toSkuResponse)
                .toList();

        return new ProductDetailResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getCategoryId()),
                category == null ? null : category.getName(),
                product.getName(),
                product.getSubTitle(),
                product.getMainImage(),
                product.getDescription(),
                product.getPrice(),
                product.getSalesCount(),
                productSupport.mapProductStatus(product.getStatus()),
                skuList,
                product.getCreateTime(),
                product.getUpdateTime()
        );
    }

    public PageResult<ProductSummaryResponse> listProducts(String categoryId, Integer pageNum, Integer pageSize) {
        Long normalizedCategoryId = normalizeOptionalLong(categoryId, "categoryId");
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        Long total = productMapper.selectCount(buildBaseQuery(normalizedCategoryId));
        if (total == null || total == 0) {
            return PageResult.of(List.of(), 0, normalizedPageNum, normalizedPageSize);
        }

        List<ProductSummaryResponse> list = productMapper.selectList(buildBaseQuery(normalizedCategoryId)
                        .orderByDesc(ProductEntity::getSalesCount)
                        .orderByDesc(ProductEntity::getId)
                        .last("LIMIT " + offset + ", " + normalizedPageSize))
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return PageResult.of(list, total, normalizedPageNum, normalizedPageSize);
    }

    public ProductSkuSnapshot getSkuSnapshot(Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "skuId 非法");
        }

        ProductSkuEntity sku = productSkuMapper.selectOne(Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getId, skuId)
                .eq(ProductSkuEntity::getIsDeleted, 0));
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }

        ProductEntity product = productMapper.selectOne(Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getId, sku.getProductId())
                .eq(ProductEntity::getIsDeleted, 0));
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        return toSkuSnapshot(product, sku);
    }

    public List<ProductSkuSnapshot> querySkuSnapshots(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }

        List<Long> normalizedSkuIds = skuIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
        if (normalizedSkuIds.isEmpty()) {
            return List.of();
        }

        List<ProductSkuEntity> skus = productSkuMapper.selectBatchIds(normalizedSkuIds).stream()
                .filter(sku -> sku.getIsDeleted() != null && sku.getIsDeleted() == 0)
                .toList();
        if (skus.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = skus.stream()
                .map(ProductSkuEntity::getProductId)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = productMapper.selectBatchIds(productIds).stream()
                .filter(product -> product.getIsDeleted() != null && product.getIsDeleted() == 0)
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        Map<Long, ProductSkuEntity> skuMap = skus.stream()
                .collect(Collectors.toMap(ProductSkuEntity::getId, Function.identity()));

        return normalizedSkuIds.stream()
                .map(skuMap::get)
                .filter(sku -> sku != null)
                .map(sku -> {
                    ProductEntity product = productMap.get(sku.getProductId());
                    return product == null ? null : toSkuSnapshot(product, sku);
                })
                .filter(snapshot -> snapshot != null)
                .toList();
    }

    private LambdaQueryWrapper<ProductEntity> buildBaseQuery(Long categoryId) {
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getIsDeleted, 0)
                .eq(ProductEntity::getStatus, 1)
                .select(
                        ProductEntity::getId,
                        ProductEntity::getCategoryId,
                        ProductEntity::getName,
                        ProductEntity::getMainImage,
                        ProductEntity::getPrice,
                        ProductEntity::getSalesCount
                );
        if (categoryId != null) {
            wrapper.eq(ProductEntity::getCategoryId, categoryId);
        }
        return wrapper;
    }

    private ProductSummaryResponse toSummaryResponse(ProductEntity product) {
        return new ProductSummaryResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getCategoryId()),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getSalesCount()
        );
    }

    private ProductSkuSnapshot toSkuSnapshot(ProductEntity product, ProductSkuEntity sku) {
        String productStatus = productSupport.mapProductStatus(product.getStatus());
        String skuStatus = productSupport.mapSkuStatus(sku.getStatus());
        boolean available = "ON_SALE".equals(productStatus)
                && "ENABLED".equals(skuStatus)
                && sku.getStock() != null
                && sku.getStock() > 0;
        return new ProductSkuSnapshot(
                sku.getId(),
                product.getId(),
                product.getName(),
                product.getMainImage(),
                sku.getSkuCode(),
                productSupport.parseSpecJson(sku.getSpecJson()),
                sku.getPrice(),
                sku.getStock(),
                productStatus,
                skuStatus,
                available
        );
    }

    private ProductSkuResponse toSkuResponse(ProductSkuEntity sku) {
        return new ProductSkuResponse(
                String.valueOf(sku.getId()),
                sku.getSkuCode(),
                productSupport.parseSpecJson(sku.getSpecJson()),
                sku.getPrice(),
                sku.getStock(),
                productSupport.mapSkuStatus(sku.getStatus())
        );
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

}
