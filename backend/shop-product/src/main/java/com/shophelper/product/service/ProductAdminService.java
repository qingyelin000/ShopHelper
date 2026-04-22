package com.shophelper.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.product.dto.AdminCategoryResponse;
import com.shophelper.product.dto.AdminCategoryUpsertRequest;
import com.shophelper.product.dto.AdminProductDetailResponse;
import com.shophelper.product.dto.AdminProductSkuResponse;
import com.shophelper.product.dto.AdminProductSkuUpsertRequest;
import com.shophelper.product.dto.AdminProductSummaryResponse;
import com.shophelper.product.dto.AdminProductUpsertRequest;
import com.shophelper.product.entity.CategoryEntity;
import com.shophelper.product.entity.ProductEntity;
import com.shophelper.product.entity.ProductSkuEntity;
import com.shophelper.product.mapper.CategoryMapper;
import com.shophelper.product.mapper.ProductMapper;
import com.shophelper.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品后台管理服务
 */
@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSupport productSupport;

    public List<AdminCategoryResponse> listCategories() {
        return categoryMapper.selectList(Wrappers.<CategoryEntity>lambdaQuery()
                        .eq(CategoryEntity::getIsDeleted, 0)
                        .orderByDesc(CategoryEntity::getSortOrder)
                        .orderByAsc(CategoryEntity::getLevel)
                        .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public AdminCategoryResponse getCategory(Long categoryId) {
        return toCategoryResponse(getActiveCategory(categoryId));
    }

    @Transactional
    public AdminCategoryResponse createCategory(AdminCategoryUpsertRequest request) {
        CategoryEntity category = new CategoryEntity();
        fillCategory(category, request, null);
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        return toCategoryResponse(category);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long categoryId, AdminCategoryUpsertRequest request) {
        CategoryEntity category = getActiveCategory(categoryId);
        fillCategory(category, request, categoryId);
        categoryMapper.updateById(category);
        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        CategoryEntity category = getActiveCategory(categoryId);
        boolean hasChildren = existsActiveChildCategory(categoryId);
        if (hasChildren) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该分类下仍存在子分类，不能删除");
        }
        boolean hasProducts = existsActiveProductUnderCategory(categoryId);
        if (hasProducts) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该分类下仍存在商品，不能删除");
        }
        category.setIsDeleted(1);
        categoryMapper.updateById(category);
    }

    public PageResult<AdminProductSummaryResponse> listProducts(String categoryId,
                                                                String status,
                                                                String keyword,
                                                                Integer pageNum,
                                                                Integer pageSize) {
        Long normalizedCategoryId = normalizeOptionalId(categoryId, "categoryId");
        Integer normalizedStatus = normalizeOptionalProductStatus(status);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        LambdaQueryWrapper<ProductEntity> baseQuery = buildAdminProductQuery(normalizedCategoryId, normalizedStatus, keyword);
        Long total = productMapper.selectCount(baseQuery);
        if (total == null || total == 0) {
            return PageResult.of(List.of(), 0, normalizedPageNum, normalizedPageSize);
        }

        List<ProductEntity> products = productMapper.selectList(buildAdminProductQuery(normalizedCategoryId, normalizedStatus, keyword)
                .orderByDesc(ProductEntity::getUpdateTime)
                .orderByDesc(ProductEntity::getId)
                .last("LIMIT " + offset + ", " + normalizedPageSize));
        Map<Long, String> categoryNameMap = loadCategoryNameMap(products.stream()
                .map(ProductEntity::getCategoryId)
                .collect(Collectors.toSet()));

        List<AdminProductSummaryResponse> list = products.stream()
                .map(product -> toAdminProductSummary(product, categoryNameMap.get(product.getCategoryId())))
                .toList();
        return PageResult.of(list, total, normalizedPageNum, normalizedPageSize);
    }

    public AdminProductDetailResponse getProduct(Long productId) {
        ProductEntity product = getActiveProduct(productId);
        CategoryEntity category = getActiveCategory(product.getCategoryId());
        List<AdminProductSkuResponse> skuList = productSkuMapper.selectList(Wrappers.<ProductSkuEntity>lambdaQuery()
                        .eq(ProductSkuEntity::getProductId, product.getId())
                        .eq(ProductSkuEntity::getIsDeleted, 0)
                        .orderByAsc(ProductSkuEntity::getId))
                .stream()
                .map(this::toAdminProductSkuResponse)
                .toList();

        return new AdminProductDetailResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getCategoryId()),
                category.getName(),
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

    @Transactional
    public AdminProductDetailResponse createProduct(AdminProductUpsertRequest request) {
        Long categoryId = normalizeRequiredId(request.getCategoryId(), "categoryId");
        CategoryEntity category = getActiveCategory(categoryId);
        ProductEntity product = new ProductEntity();
        fillProduct(product, request, category);
        product.setPrice(BigDecimal.ZERO);
        product.setSalesCount(0);
        product.setIsDeleted(0);
        if (product.getStatus() != null && product.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品未配置可售 SKU，不能直接上架");
        }
        productMapper.insert(product);
        return getProduct(product.getId());
    }

    @Transactional
    public AdminProductDetailResponse updateProduct(Long productId, AdminProductUpsertRequest request) {
        ProductEntity product = getActiveProduct(productId);
        CategoryEntity category = getActiveCategory(normalizeRequiredId(request.getCategoryId(), "categoryId"));
        fillProduct(product, request, category);
        ensureProductCanBeOnSale(product.getId(), product.getStatus());
        productMapper.updateById(product);
        refreshDisplayPrice(product.getId());
        return getProduct(product.getId());
    }

    @Transactional
    public void deleteProduct(Long productId) {
        ProductEntity product = getActiveProduct(productId);
        product.setIsDeleted(1);
        product.setStatus(2);
        productMapper.updateById(product);
        productSkuMapper.update(null, Wrappers.<ProductSkuEntity>lambdaUpdate()
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getIsDeleted, 0)
                .set(ProductSkuEntity::getIsDeleted, 1)
                .set(ProductSkuEntity::getStatus, 0));
    }

    @Transactional
    public AdminProductSkuResponse createSku(Long productId, AdminProductSkuUpsertRequest request) {
        ProductEntity product = getActiveProduct(productId);
        ProductSkuEntity sku = new ProductSkuEntity();
        fillSku(sku, product.getId(), request, null);
        sku.setVersion(0);
        sku.setIsDeleted(0);
        productSkuMapper.insert(sku);
        refreshDisplayPrice(productId);
        return toAdminProductSkuResponse(sku);
    }

    @Transactional
    public AdminProductSkuResponse updateSku(Long productId, Long skuId, AdminProductSkuUpsertRequest request) {
        getActiveProduct(productId);
        ProductSkuEntity sku = getActiveSku(productId, skuId);
        guardProductOnSaleBeforeDisablingLastEnabledSku(productId, skuId, request.getStatus());
        fillSku(sku, productId, request, skuId);
        productSkuMapper.updateById(sku);
        refreshDisplayPrice(productId);
        return toAdminProductSkuResponse(sku);
    }

    @Transactional
    public void deleteSku(Long productId, Long skuId) {
        getActiveProduct(productId);
        ProductSkuEntity sku = getActiveSku(productId, skuId);
        guardProductOnSaleBeforeRemovingLastEnabledSku(productId, skuId);
        sku.setIsDeleted(1);
        sku.setStatus(0);
        productSkuMapper.updateById(sku);
        refreshDisplayPrice(productId);
    }

    private void fillCategory(CategoryEntity category, AdminCategoryUpsertRequest request, Long selfCategoryId) {
        Long parentId = normalizeOptionalId(request.getParentId(), "parentId");
        CategoryEntity parent = null;
        int level = 1;
        if (parentId != null && parentId > 0) {
            parent = getActiveCategory(parentId);
            if (selfCategoryId != null && selfCategoryId.equals(parentId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不能挂到自己下面");
            }
            if (selfCategoryId != null) {
                assertNoCycle(parent, selfCategoryId);
            }
            level = parent.getLevel() + 1;
        }
        if (level < 1 || level > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类层级仅支持 1-3 级");
        }

        category.setParentId(parent == null ? 0L : parent.getId());
        category.setName(request.getName().trim());
        category.setIconUrl(normalizeNullableText(request.getIconUrl()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setLevel(level);
        category.setStatus(productSupport.parseCategoryStatus(request.getStatus()));
    }

    private void fillProduct(ProductEntity product, AdminProductUpsertRequest request, CategoryEntity category) {
        product.setCategoryId(category.getId());
        product.setName(request.getName().trim());
        product.setSubTitle(normalizeNullableText(request.getSubTitle()));
        product.setMainImage(normalizeNullableText(request.getMainImage()));
        product.setDescription(normalizeNullableText(request.getDescription()));
        product.setStatus(productSupport.parseProductStatus(request.getStatus()));
    }

    private void fillSku(ProductSkuEntity sku, Long productId, AdminProductSkuUpsertRequest request, Long selfSkuId) {
        if (request.getStock() != null && request.getStock() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "stock 不能小于 0");
        }
        String skuCode = request.getSkuCode().trim();
        ensureSkuCodeUnique(skuCode, selfSkuId);

        String specJson = productSupport.toSpecJson(request.getSpec());
        String specHash = productSupport.buildSpecHash(request.getSpec());
        ensureSkuSpecUnique(productId, specHash, selfSkuId);

        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSpecJson(specJson);
        sku.setSpecHash(specHash);
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setStatus(productSupport.parseSkuStatus(request.getStatus()));
    }

    private void assertNoCycle(CategoryEntity parent, Long selfCategoryId) {
        CategoryEntity current = parent;
        while (current != null && current.getParentId() != null && current.getParentId() > 0) {
            if (selfCategoryId.equals(current.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "父分类不能是当前分类的子孙节点");
            }
            current = categoryMapper.selectOne(Wrappers.<CategoryEntity>lambdaQuery()
                    .eq(CategoryEntity::getId, current.getParentId())
                    .eq(CategoryEntity::getIsDeleted, 0));
        }
        if (current != null && selfCategoryId.equals(current.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "父分类不能是当前分类的子孙节点");
        }
    }

    private void ensureSkuCodeUnique(String skuCode, Long selfSkuId) {
        ProductSkuEntity existing = productSkuMapper.selectOne(Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getSkuCode, skuCode)
                .last("LIMIT 1"));
        if (existing != null && (selfSkuId == null || !selfSkuId.equals(existing.getId()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "skuCode 已存在");
        }
    }

    private void ensureSkuSpecUnique(Long productId, String specHash, Long selfSkuId) {
        ProductSkuEntity existing = productSkuMapper.selectOne(Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getSpecHash, specHash)
                .last("LIMIT 1"));
        if (existing != null && (selfSkuId == null || !selfSkuId.equals(existing.getId()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "同一商品下 SKU 规格组合不能重复");
        }
    }

    private void ensureProductCanBeOnSale(Long productId, Integer targetStatus) {
        if (targetStatus == null || targetStatus != 1) {
            return;
        }
        Long enabledSkuCount = countEnabledSku(productId, null);
        if (enabledSkuCount == null || enabledSkuCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品至少需要一个启用 SKU 才能上架");
        }
    }

    private void guardProductOnSaleBeforeDisablingLastEnabledSku(Long productId, Long skuId, String targetStatus) {
        if (productSupport.parseSkuStatus(targetStatus) == 1) {
            return;
        }
        ProductEntity product = getActiveProduct(productId);
        if (product.getStatus() == null || product.getStatus() != 1) {
            return;
        }
        Long enabledSkuCount = countEnabledSku(productId, skuId);
        if (enabledSkuCount == null || enabledSkuCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已上架，至少保留一个启用 SKU；请先下架商品");
        }
    }

    private void guardProductOnSaleBeforeRemovingLastEnabledSku(Long productId, Long skuId) {
        ProductSkuEntity sku = getActiveSku(productId, skuId);
        if (sku.getStatus() == null || sku.getStatus() != 1) {
            return;
        }
        ProductEntity product = getActiveProduct(productId);
        if (product.getStatus() == null || product.getStatus() != 1) {
            return;
        }
        Long enabledSkuCount = countEnabledSku(productId, skuId);
        if (enabledSkuCount == null || enabledSkuCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已上架，至少保留一个启用 SKU；请先下架商品");
        }
    }

    private Long countEnabledSku(Long productId, Long excludeSkuId) {
        LambdaQueryWrapper<ProductSkuEntity> query = Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getIsDeleted, 0)
                .eq(ProductSkuEntity::getStatus, 1);
        if (excludeSkuId != null) {
            query.ne(ProductSkuEntity::getId, excludeSkuId);
        }
        return productSkuMapper.selectCount(query);
    }

    private void refreshDisplayPrice(Long productId) {
        List<ProductSkuEntity> enabledSkus = productSkuMapper.selectList(Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getIsDeleted, 0)
                .eq(ProductSkuEntity::getStatus, 1)
                .select(ProductSkuEntity::getPrice));
        BigDecimal displayPrice = enabledSkus.stream()
                .map(ProductSkuEntity::getPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        productMapper.update(null, Wrappers.<ProductEntity>lambdaUpdate()
                .eq(ProductEntity::getId, productId)
                .set(ProductEntity::getPrice, displayPrice));
    }

    private LambdaQueryWrapper<ProductEntity> buildAdminProductQuery(Long categoryId, Integer status, String keyword) {
        LambdaQueryWrapper<ProductEntity> query = Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getIsDeleted, 0);
        if (categoryId != null) {
            query.eq(ProductEntity::getCategoryId, categoryId);
        }
        if (status != null) {
            query.eq(ProductEntity::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(ProductEntity::getName, keyword.trim());
        }
        return query;
    }

    private Map<Long, String> loadCategoryNameMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .filter(category -> category.getIsDeleted() != null && category.getIsDeleted() == 0)
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));
    }

    private boolean existsActiveChildCategory(Long categoryId) {
        Long count = categoryMapper.selectCount(Wrappers.<CategoryEntity>lambdaQuery()
                .eq(CategoryEntity::getParentId, categoryId)
                .eq(CategoryEntity::getIsDeleted, 0));
        return count != null && count > 0;
    }

    private boolean existsActiveProductUnderCategory(Long categoryId) {
        Long count = productMapper.selectCount(Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getCategoryId, categoryId)
                .eq(ProductEntity::getIsDeleted, 0));
        return count != null && count > 0;
    }

    private CategoryEntity getActiveCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "categoryId 非法");
        }
        CategoryEntity category = categoryMapper.selectOne(Wrappers.<CategoryEntity>lambdaQuery()
                .eq(CategoryEntity::getId, categoryId)
                .eq(CategoryEntity::getIsDeleted, 0));
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private ProductEntity getActiveProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "productId 非法");
        }
        ProductEntity product = productMapper.selectOne(Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getId, productId)
                .eq(ProductEntity::getIsDeleted, 0));
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    private ProductSkuEntity getActiveSku(Long productId, Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "skuId 非法");
        }
        ProductSkuEntity sku = productSkuMapper.selectOne(Wrappers.<ProductSkuEntity>lambdaQuery()
                .eq(ProductSkuEntity::getId, skuId)
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getIsDeleted, 0));
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        return sku;
    }

    private Long normalizeRequiredId(String value, String fieldName) {
        Long parsed = normalizeOptionalId(value, fieldName);
        if (parsed == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        return parsed;
    }

    private Long normalizeOptionalId(String value, String fieldName) {
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

    private Integer normalizeOptionalProductStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return productSupport.parseProductStatus(status);
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

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AdminCategoryResponse toCategoryResponse(CategoryEntity category) {
        return new AdminCategoryResponse(
                String.valueOf(category.getId()),
                String.valueOf(category.getParentId()),
                category.getName(),
                category.getIconUrl(),
                category.getSortOrder(),
                category.getLevel(),
                productSupport.mapCategoryStatus(category.getStatus()),
                category.getCreateTime(),
                category.getUpdateTime()
        );
    }

    private AdminProductSummaryResponse toAdminProductSummary(ProductEntity product, String categoryName) {
        return new AdminProductSummaryResponse(
                String.valueOf(product.getId()),
                String.valueOf(product.getCategoryId()),
                categoryName,
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getSalesCount(),
                productSupport.mapProductStatus(product.getStatus()),
                product.getCreateTime(),
                product.getUpdateTime()
        );
    }

    private AdminProductSkuResponse toAdminProductSkuResponse(ProductSkuEntity sku) {
        return new AdminProductSkuResponse(
                String.valueOf(sku.getId()),
                String.valueOf(sku.getProductId()),
                sku.getSkuCode(),
                productSupport.parseSpecJson(sku.getSpecJson()),
                sku.getSpecHash(),
                sku.getPrice(),
                sku.getStock(),
                sku.getVersion(),
                productSupport.mapSkuStatus(sku.getStatus()),
                sku.getCreateTime(),
                sku.getUpdateTime()
        );
    }
}
