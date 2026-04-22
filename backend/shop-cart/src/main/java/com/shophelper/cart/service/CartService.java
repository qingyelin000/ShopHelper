package com.shophelper.cart.service;

import com.shophelper.cart.dto.AddCartItemRequest;
import com.shophelper.cart.dto.CartItemResponse;
import com.shophelper.cart.dto.CartSnapshotResponse;
import com.shophelper.cart.dto.UpdateCartItemRequest;
import com.shophelper.cart.model.CartRedisItem;
import com.shophelper.common.core.dto.product.ProductSkuSnapshot;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Duration CART_TTL = Duration.ofDays(30);

    private final RedissonClient redissonClient;
    private final ProductSnapshotGateway productSnapshotGateway;

    public CartSnapshotResponse getCart(Long userId) {
        return refreshAndBuildSnapshot(userId);
    }

    public CartSnapshotResponse addItem(Long userId, AddCartItemRequest request) {
        Long skuId = normalizeSkuId(request.getSkuId());
        ProductSkuSnapshot snapshot = productSnapshotGateway.getSkuSnapshot(skuId);
        assertSkuCanBeAdded(snapshot, request.getQuantity());

        RMapCache<String, CartRedisItem> cart = getCartMap(userId);
        String itemId = String.valueOf(skuId);
        CartRedisItem item = cart.get(itemId);
        int targetQuantity = request.getQuantity();
        if (item != null) {
            targetQuantity += item.getQuantity();
        }
        assertStockEnough(snapshot, targetQuantity);

        CartRedisItem updatedItem = item == null ? new CartRedisItem() : item;
        fillItemFromSnapshot(updatedItem, snapshot);
        updatedItem.setItemId(itemId);
        updatedItem.setQuantity(targetQuantity);
        updatedItem.setSelected(true);
        updatedItem.setAvailable(true);
        updatedItem.setUpdatedAt(System.currentTimeMillis());
        cart.fastPut(itemId, updatedItem);
        touch(cart);

        return refreshAndBuildSnapshot(userId);
    }

    public CartSnapshotResponse updateItem(Long userId, String itemId, UpdateCartItemRequest request) {
        if (request.getQuantity() == null && request.getSelected() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "quantity 和 selected 不能同时为空");
        }

        RMapCache<String, CartRedisItem> cart = getCartMap(userId);
        CartRedisItem item = cart.get(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车项不存在");
        }

        ProductSkuSnapshot snapshot = productSnapshotGateway.getSkuSnapshot(normalizeSkuId(item.getSkuId()));
        int targetQuantity = request.getQuantity() == null ? item.getQuantity() : request.getQuantity();
        assertSkuCanBeAdjusted(snapshot, targetQuantity);

        boolean available = isCartItemAvailable(snapshot, targetQuantity);
        if (Boolean.TRUE.equals(request.getSelected()) && !available) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架或库存不足，不能选择结算");
        }

        fillItemFromSnapshot(item, snapshot);
        item.setQuantity(targetQuantity);
        item.setAvailable(available);
        item.setSelected(available && (request.getSelected() == null ? item.isSelected() : request.getSelected()));
        item.setUpdatedAt(System.currentTimeMillis());
        cart.fastPut(itemId, item);
        touch(cart);

        return refreshAndBuildSnapshot(userId);
    }

    public CartSnapshotResponse removeItem(Long userId, String itemId) {
        RMapCache<String, CartRedisItem> cart = getCartMap(userId);
        if (!cart.containsKey(itemId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车项不存在");
        }
        cart.fastRemove(itemId);
        touch(cart);
        return refreshAndBuildSnapshot(userId);
    }

    public CartSnapshotResponse clear(Long userId) {
        RMapCache<String, CartRedisItem> cart = getCartMap(userId);
        cart.delete();
        return emptySnapshot();
    }

    private CartSnapshotResponse refreshAndBuildSnapshot(Long userId) {
        RMapCache<String, CartRedisItem> cart = getCartMap(userId);
        touch(cart);
        Map<String, CartRedisItem> storedItems = cart.readAllMap();
        if (storedItems.isEmpty()) {
            return emptySnapshot();
        }

        List<Long> skuIds = storedItems.values().stream()
                .map(CartRedisItem::getSkuId)
                .map(this::normalizeSkuId)
                .toList();
        Map<Long, ProductSkuSnapshot> snapshotMap = productSnapshotGateway.querySkuSnapshots(skuIds).stream()
                .collect(Collectors.toMap(ProductSkuSnapshot::getSkuId, Function.identity()));

        List<CartRedisItem> refreshedItems = new ArrayList<>();
        for (CartRedisItem item : storedItems.values()) {
            ProductSkuSnapshot snapshot = snapshotMap.get(normalizeSkuId(item.getSkuId()));
            if (snapshot == null) {
                item.setStock(0);
                item.setAvailable(false);
                item.setSelected(false);
            } else {
                fillItemFromSnapshot(item, snapshot);
                boolean available = isCartItemAvailable(snapshot, item.getQuantity());
                item.setAvailable(available);
                if (!available) {
                    item.setSelected(false);
                }
            }
            refreshedItems.add(item);
            cart.fastPut(item.getItemId(), item);
        }

        return buildSnapshot(refreshedItems);
    }

    private void fillItemFromSnapshot(CartRedisItem item, ProductSkuSnapshot snapshot) {
        item.setSkuId(String.valueOf(snapshot.getSkuId()));
        item.setProductId(String.valueOf(snapshot.getProductId()));
        item.setProductName(snapshot.getProductName());
        item.setProductImage(snapshot.getProductImage());
        item.setSkuCode(snapshot.getSkuCode());
        item.setSkuSpec(snapshot.getSkuSpec());
        item.setUnitPrice(snapshot.getUnitPrice());
        item.setStock(snapshot.getStock());
        item.setProductStatus(snapshot.getProductStatus());
        item.setSkuStatus(snapshot.getSkuStatus());
    }

    private boolean isCartItemAvailable(ProductSkuSnapshot snapshot, Integer quantity) {
        return snapshot.isAvailable()
                && snapshot.getStock() != null
                && quantity != null
                && quantity > 0
                && snapshot.getStock() >= quantity;
    }

    private void assertSkuCanBeAdded(ProductSkuSnapshot snapshot, Integer quantity) {
        if (!snapshot.isAvailable()) {
            if (snapshot.getStock() == null || snapshot.getStock() <= 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足，当前可购数量为 0");
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架或不可售");
        }
        assertStockEnough(snapshot, quantity);
    }

    private void assertSkuCanBeAdjusted(ProductSkuSnapshot snapshot, Integer quantity) {
        if (!"ON_SALE".equals(snapshot.getProductStatus()) || !"ENABLED".equals(snapshot.getSkuStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架或不可售");
        }
        assertStockEnough(snapshot, quantity);
    }

    private void assertStockEnough(ProductSkuSnapshot snapshot, Integer quantity) {
        int currentStock = snapshot.getStock() == null ? 0 : snapshot.getStock();
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "quantity 必须大于 0");
        }
        if (currentStock < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足，当前可购数量为 " + currentStock);
        }
    }

    private CartSnapshotResponse buildSnapshot(List<CartRedisItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(CartRedisItem::getUpdatedAt, Comparator.nullsLast(Long::compareTo)).reversed())
                .map(this::toCartItemResponse)
                .toList();
        int selectedCount = (int) items.stream()
                .filter(CartRedisItem::isSelected)
                .count();
        int totalQuantity = items.stream()
                .map(CartRedisItem::getQuantity)
                .reduce(0, Integer::sum);
        BigDecimal estimatedTotal = itemResponses.stream()
                .filter(CartItemResponse::isSelected)
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new CartSnapshotResponse(itemResponses, selectedCount, totalQuantity, estimatedTotal);
    }

    private CartItemResponse toCartItemResponse(CartRedisItem item) {
        BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new CartItemResponse(
                item.getItemId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductImage(),
                item.getSkuId(),
                item.getSkuSpec(),
                unitPrice,
                item.getQuantity(),
                totalPrice,
                item.getStock(),
                item.isSelected(),
                item.isAvailable()
        );
    }

    private Long normalizeSkuId(String skuId) {
        try {
            long parsed = Long.parseLong(skuId.trim());
            if (parsed <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "skuId 非法");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "skuId 格式非法");
        }
    }

    private RMapCache<String, CartRedisItem> getCartMap(Long userId) {
        return redissonClient.getMapCache("cart:" + userId);
    }

    private void touch(RMapCache<String, CartRedisItem> cart) {
        cart.expire(CART_TTL);
    }

    private CartSnapshotResponse emptySnapshot() {
        return new CartSnapshotResponse(List.of(), 0, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
}
