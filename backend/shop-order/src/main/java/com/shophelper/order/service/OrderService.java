package com.shophelper.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.common.core.util.PhoneSecurityUtils;
import com.shophelper.order.config.OrderUserSecurityProperties;
import com.shophelper.order.dto.CancelOrderRequest;
import com.shophelper.order.dto.CancelOrderResponse;
import com.shophelper.order.dto.CreateOrderRequest;
import com.shophelper.order.dto.CreateOrderResponse;
import com.shophelper.order.dto.OrderDetailResponse;
import com.shophelper.order.dto.OrderSummaryResponse;
import com.shophelper.order.dto.PayOrderRequest;
import com.shophelper.order.dto.PayOrderResponse;
import com.shophelper.order.entity.OrderEntity;
import com.shophelper.order.entity.OrderItemEntity;
import com.shophelper.order.entity.ProductEntity;
import com.shophelper.order.entity.ProductSkuEntity;
import com.shophelper.order.entity.UserAddressEntity;
import com.shophelper.order.mapper.OrderItemMapper;
import com.shophelper.order.mapper.OrderMapper;
import com.shophelper.order.mapper.ProductMapper;
import com.shophelper.order.mapper.ProductSkuMapper;
import com.shophelper.order.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(1);
    private static final Duration CART_TTL = Duration.ofDays(30);
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserAddressMapper userAddressMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OrderUserSecurityProperties orderUserSecurityProperties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request, String idempotencyKey) {
        ensureIdempotencyKeyPresent(idempotencyKey);
        String key = "order:create:" + userId + ":" + idempotencyKey.trim();
        RLock lock = redissonClient.getLock(key + ":lock");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            Long existingOrderId = getIdempotentOrderId(key);
            if (existingOrderId != null) {
                return toCreateOrderResponse(getOrderEntity(userId, existingOrderId));
            }

            UserAddressEntity address = getUserAddress(userId, normalizeId(request.getAddressId(), "addressId"));
            List<NormalizedOrderItem> normalizedItems = normalizeItems(request.getItems());
            Map<Long, ProductSkuEntity> skuMap = loadSkuMap(normalizedItems);
            Map<Long, ProductEntity> productMap = loadProductMap(skuMap.values());

            List<OrderItemEntity> orderItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (NormalizedOrderItem item : normalizedItems) {
                ProductSkuEntity sku = requireSku(skuMap, item.skuId());
                ProductEntity product = requireProduct(productMap, sku.getProductId());
                ensureSkuCanCreateOrder(product, sku, item.quantity());
                if (productSkuMapper.deductStock(sku.getId(), item.quantity(), sku.getVersion()) != 1) {
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                            "商品「" + product.getName() + "」库存不足，请刷新后重试");
                }

                BigDecimal unitPrice = normalizeMoney(sku.getPrice());
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                totalAmount = totalAmount.add(itemTotal);

                OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setUserId(userId);
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getMainImage());
                orderItem.setSkuId(sku.getId());
                orderItem.setSkuCode(sku.getSkuCode());
                orderItem.setSkuSpecJson(sku.getSpecJson());
                orderItem.setUnitPrice(unitPrice);
                orderItem.setQuantity(item.quantity());
                orderItem.setTotalPrice(itemTotal);
                orderItem.setIsDeleted(0);
                orderItems.add(orderItem);
            }

            OrderEntity order = new OrderEntity();
            order.setOrderNo(buildOrderNo(userId));
            order.setUserId(userId);
            order.setAddressId(address.getId());
            order.setSnapReceiverName(address.getReceiverName());
            order.setSnapReceiverPhone(address.getReceiverPhone());
            order.setSnapProvince(address.getProvince());
            order.setSnapCity(address.getCity());
            order.setSnapDistrict(address.getDistrict());
            order.setSnapDetailAddress(address.getDetailAddress());
            order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
            order.setPayAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
            order.setFreightAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            order.setPayType(null);
            order.setSource(normalizeSource(request.getSource()));
            order.setStatus(CommonConstants.ORDER_STATUS_PENDING_PAYMENT);
            order.setCancelReason(null);
            order.setRemark(normalizeRemark(request.getRemark()));
            order.setExpireTime(LocalDateTime.now().plusMinutes(30));
            order.setPayTime(null);
            order.setIsDeleted(0);
            orderMapper.insert(order);

            for (OrderItemEntity orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
                orderItem.setOrderNo(order.getOrderNo());
                orderItemMapper.insert(orderItem);
            }

            cleanupCart(userId, normalizedItems.stream()
                    .map(item -> String.valueOf(item.skuId()))
                    .toList());
            saveIdempotentOrderId(key, order.getId());
            return toCreateOrderResponse(order);
        } finally {
            unlock(lock);
        }
    }

    public PageResult<OrderSummaryResponse> listOrders(Long userId, String status, Integer pageNum, Integer pageSize) {
        Integer normalizedStatus = normalizeOptionalOrderStatus(status);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        LambdaQueryWrapper<OrderEntity> baseQuery = Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getIsDeleted, 0);
        if (normalizedStatus != null) {
            baseQuery.eq(OrderEntity::getStatus, normalizedStatus);
        }

        Long total = orderMapper.selectCount(baseQuery);
        if (total == null || total == 0) {
            return PageResult.of(List.of(), 0, normalizedPageNum, normalizedPageSize);
        }

        List<OrderEntity> orders = orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getIsDeleted, 0)
                .eq(normalizedStatus != null, OrderEntity::getStatus, normalizedStatus)
                .orderByDesc(OrderEntity::getCreateTime)
                .orderByDesc(OrderEntity::getId)
                .last("LIMIT " + offset + ", " + normalizedPageSize));
        Map<Long, List<OrderItemEntity>> itemMap = loadOrderItems(orders.stream().map(OrderEntity::getId).toList());

        List<OrderSummaryResponse> list = orders.stream()
                .map(order -> toSummaryResponse(order, itemMap.getOrDefault(order.getId(), List.of())))
                .toList();
        return PageResult.of(list, total, normalizedPageNum, normalizedPageSize);
    }

    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        OrderEntity order = getOrderEntity(userId, orderId);
        List<OrderItemEntity> items = orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
                .eq(OrderItemEntity::getOrderId, order.getId())
                .eq(OrderItemEntity::getUserId, userId)
                .eq(OrderItemEntity::getIsDeleted, 0)
                .orderByAsc(OrderItemEntity::getId));
        return toDetailResponse(order, items);
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        OrderEntity order = getOrderEntity(userId, orderId);
        ensureOrderCancelable(order);
        restoreStock(order.getId(), userId);

        LocalDateTime now = LocalDateTime.now();
        String refundNote = null;
        if (order.getStatus() == CommonConstants.ORDER_STATUS_PAID) {
            order.setStatus(CommonConstants.ORDER_STATUS_REFUNDING);
            refundNote = "订单已取消，退款预计 3~5 个工作日到账";
        } else {
            order.setStatus(CommonConstants.ORDER_STATUS_CANCELLED);
        }
        order.setCancelReason(request.getReason().trim());
        orderMapper.updateById(order);
        return new CancelOrderResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                mapOrderStatus(order.getStatus()),
                now,
                refundNote
        );
    }

    @Transactional
    public PayOrderResponse payOrder(Long userId, Long orderId, PayOrderRequest request, String idempotencyKey) {
        ensureIdempotencyKeyPresent(idempotencyKey);
        String key = "order:pay:" + userId + ":" + idempotencyKey.trim();
        RLock lock = redissonClient.getLock(key + ":lock");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            Long existingOrderId = getIdempotentOrderId(key);
            if (existingOrderId != null) {
                OrderEntity paidOrder = getOrderEntity(userId, existingOrderId);
                return toPayOrderResponse(paidOrder, request.getPaymentMethod());
            }

            OrderEntity order = getOrderEntity(userId, orderId);
            if (order.getStatus() != CommonConstants.ORDER_STATUS_PENDING_PAYMENT) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "当前订单状态不允许支付");
            }

            Integer payType = parsePaymentMethod(request.getPaymentMethod());
            order.setPayType(payType);
            order.setStatus(CommonConstants.ORDER_STATUS_PAID);
            order.setPayTime(LocalDateTime.now());
            orderMapper.updateById(order);
            saveIdempotentOrderId(key, order.getId());
            return toPayOrderResponse(order, request.getPaymentMethod());
        } finally {
            unlock(lock);
        }
    }

    @Transactional
    public void confirmOrder(Long userId, Long orderId) {
        OrderEntity order = getOrderEntity(userId, orderId);
        if (order.getStatus() != CommonConstants.ORDER_STATUS_SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "当前订单状态不允许确认收货");
        }
        order.setStatus(CommonConstants.ORDER_STATUS_COMPLETED);
        orderMapper.updateById(order);
    }

    private Map<Long, List<OrderItemEntity>> loadOrderItems(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
                        .in(OrderItemEntity::getOrderId, orderIds)
                        .eq(OrderItemEntity::getIsDeleted, 0)
                        .orderByAsc(OrderItemEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
    }

    private Map<Long, ProductSkuEntity> loadSkuMap(List<NormalizedOrderItem> items) {
        Set<Long> skuIds = items.stream().map(NormalizedOrderItem::skuId).collect(Collectors.toSet());
        return productSkuMapper.selectBatchIds(skuIds).stream()
                .filter(sku -> sku.getIsDeleted() == null || sku.getIsDeleted() == 0)
                .collect(Collectors.toMap(ProductSkuEntity::getId, Function.identity()));
    }

    private Map<Long, ProductEntity> loadProductMap(Iterable<ProductSkuEntity> skus) {
        Set<Long> productIds = new java.util.HashSet<>();
        for (ProductSkuEntity sku : skus) {
            productIds.add(sku.getProductId());
        }
        return productMapper.selectBatchIds(productIds).stream()
                .filter(product -> product.getIsDeleted() == null || product.getIsDeleted() == 0)
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    private UserAddressEntity getUserAddress(Long userId, Long addressId) {
        UserAddressEntity address = userAddressMapper.selectOne(Wrappers.<UserAddressEntity>lambdaQuery()
                .eq(UserAddressEntity::getId, addressId)
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getIsDeleted, 0));
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private OrderEntity getOrderEntity(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectOne(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getId, orderId)
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getIsDeleted, 0));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void ensureSkuCanCreateOrder(ProductEntity product, ProductSkuEntity sku, Integer quantity) {
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架或不可售");
        }
        if (sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架或不可售");
        }
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "quantity 必须大于 0");
        }
        int currentStock = sku.getStock() == null ? 0 : sku.getStock();
        if (currentStock < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                    "商品「" + product.getName() + "」库存不足");
        }
    }

    private void ensureOrderCancelable(OrderEntity order) {
        if (order.getStatus() == CommonConstants.ORDER_STATUS_PENDING_PAYMENT
                || order.getStatus() == CommonConstants.ORDER_STATUS_PAID) {
            return;
        }
        throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "当前订单状态不允许取消");
    }

    private void restoreStock(Long orderId, Long userId) {
        List<OrderItemEntity> items = orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
                .eq(OrderItemEntity::getOrderId, orderId)
                .eq(OrderItemEntity::getUserId, userId)
                .eq(OrderItemEntity::getIsDeleted, 0));
        for (OrderItemEntity item : items) {
            productSkuMapper.restoreStock(item.getSkuId(), item.getQuantity());
        }
    }

    private List<NormalizedOrderItem> normalizeItems(List<CreateOrderRequest.Item> rawItems) {
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : rawItems) {
            Long skuId = normalizeId(item.getSkuId(), "skuId");
            Integer quantity = item.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "quantity 必须大于 0");
            }
            quantityMap.merge(skuId, quantity, Integer::sum);
        }
        return quantityMap.entrySet().stream()
                .map(entry -> new NormalizedOrderItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private ProductSkuEntity requireSku(Map<Long, ProductSkuEntity> skuMap, Long skuId) {
        ProductSkuEntity sku = skuMap.get(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        return sku;
    }

    private ProductEntity requireProduct(Map<Long, ProductEntity> productMap, Long productId) {
        ProductEntity product = productMap.get(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    private CreateOrderResponse toCreateOrderResponse(OrderEntity order) {
        return new CreateOrderResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                mapOrderStatus(order.getStatus()),
                normalizeMoney(order.getTotalAmount()),
                normalizeMoney(order.getPayAmount()),
                normalizeMoney(order.getFreightAmount()),
                order.getExpireTime(),
                order.getCreateTime()
        );
    }

    private OrderSummaryResponse toSummaryResponse(OrderEntity order, List<OrderItemEntity> items) {
        String coverImage = items.stream()
                .sorted(Comparator.comparing(OrderItemEntity::getId))
                .map(OrderItemEntity::getProductImage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        int itemCount = items.stream()
                .map(OrderItemEntity::getQuantity)
                .reduce(0, Integer::sum);
        return new OrderSummaryResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                mapOrderStatus(order.getStatus()),
                normalizeMoney(order.getPayAmount()),
                itemCount,
                coverImage,
                order.getCreateTime(),
                order.getExpireTime()
        );
    }

    private OrderDetailResponse toDetailResponse(OrderEntity order, List<OrderItemEntity> items) {
        String phone = PhoneSecurityUtils.decryptFromBase64(
                order.getSnapReceiverPhone(),
                orderUserSecurityProperties.getPhoneEncryptSecret()
        );
        List<OrderDetailResponse.Item> orderItems = items.stream()
                .map(this::toDetailItem)
                .toList();
        return new OrderDetailResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                mapOrderStatus(order.getStatus()),
                normalizeMoney(order.getTotalAmount()),
                normalizeMoney(order.getFreightAmount()),
                normalizeMoney(order.getPayAmount()),
                mapPayType(order.getPayType()),
                order.getSource(),
                order.getRemark(),
                new OrderDetailResponse.Address(
                        order.getSnapReceiverName(),
                        PhoneSecurityUtils.maskChinaPhone(phone),
                        order.getSnapProvince() + order.getSnapCity() + order.getSnapDistrict() + order.getSnapDetailAddress()
                ),
                orderItems,
                null,
                order.getCreateTime(),
                order.getPayTime()
        );
    }

    private OrderDetailResponse.Item toDetailItem(OrderItemEntity item) {
        return new OrderDetailResponse.Item(
                String.valueOf(item.getId()),
                String.valueOf(item.getProductId()),
                item.getProductName(),
                item.getProductImage(),
                parseSpecJson(item.getSkuSpecJson()),
                normalizeMoney(item.getUnitPrice()),
                item.getQuantity(),
                normalizeMoney(item.getTotalPrice())
        );
    }

    private PayOrderResponse toPayOrderResponse(OrderEntity order, String paymentMethod) {
        String normalizedMethod = normalizePaymentMethodLabel(paymentMethod);
        String note = "MOCK".equals(normalizedMethod) ? "MOCK支付，订单将自动标记为已支付" : null;
        return new PayOrderResponse(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                normalizedMethod,
                normalizedMethod.toLowerCase() + "_session_" + order.getId(),
                "MOCK".equals(normalizedMethod) ? null : "https://pay.shophelper.local/session/" + order.getOrderNo(),
                null,
                note
        );
    }

    private Map<String, Object> parseSpecJson(String specJson) {
        if (!StringUtils.hasText(specJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(specJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单规格快照格式非法");
        }
    }

    private String buildOrderNo(Long userId) {
        String date = LocalDateTime.now().format(ORDER_NO_DATE);
        String shard = String.format("%02d", Math.floorMod(userId.intValue(), 16));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return date + shard + System.currentTimeMillis() + random;
    }

    private String mapOrderStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING_PAYMENT";
            case 1 -> "PAID";
            case 2 -> "SHIPPED";
            case 3 -> "COMPLETED";
            case 4 -> "CANCELLED";
            case 5 -> "REFUNDING";
            case 6 -> "REFUNDED";
            default -> "UNKNOWN";
        };
    }

    private Integer normalizeOptionalOrderStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.trim().toUpperCase()) {
            case "PENDING_PAYMENT" -> CommonConstants.ORDER_STATUS_PENDING_PAYMENT;
            case "PAID" -> CommonConstants.ORDER_STATUS_PAID;
            case "SHIPPED" -> CommonConstants.ORDER_STATUS_SHIPPED;
            case "COMPLETED" -> CommonConstants.ORDER_STATUS_COMPLETED;
            case "CANCELLED" -> CommonConstants.ORDER_STATUS_CANCELLED;
            case "REFUNDING" -> CommonConstants.ORDER_STATUS_REFUNDING;
            case "REFUNDED" -> CommonConstants.ORDER_STATUS_REFUNDED;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "status 仅支持 PENDING_PAYMENT / PAID / SHIPPED / COMPLETED / CANCELLED / REFUNDING / REFUNDED");
        };
    }

    private Integer parsePaymentMethod(String paymentMethod) {
        String normalized = normalizePaymentMethodLabel(paymentMethod);
        return switch (normalized) {
            case "ALIPAY" -> 1;
            case "WECHAT" -> 2;
            case "MOCK" -> 3;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "paymentMethod 仅支持 ALIPAY / WECHAT / MOCK");
        };
    }

    private String normalizePaymentMethodLabel(String paymentMethod) {
        if (!StringUtils.hasText(paymentMethod)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "paymentMethod 不能为空");
        }
        String normalized = paymentMethod.trim().toUpperCase();
        if (!normalized.equals("ALIPAY") && !normalized.equals("WECHAT") && !normalized.equals("MOCK")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "paymentMethod 仅支持 ALIPAY / WECHAT / MOCK");
        }
        return normalized;
    }

    private String mapPayType(Integer payType) {
        if (payType == null) {
            return null;
        }
        return switch (payType) {
            case 1 -> "ALIPAY";
            case 2 -> "WECHAT";
            case 3 -> "MOCK";
            default -> null;
        };
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return "web";
        }
        String normalized = source.trim().toLowerCase();
        if (!normalized.equals("app") && !normalized.equals("web") && !normalized.equals("mini")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "source 仅支持 app / web / mini");
        }
        return normalized;
    }

    private String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }

    private Long normalizeId(String rawId, String fieldName) {
        try {
            long parsed = Long.parseLong(rawId.trim());
            if (parsed <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 非法");
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
        if (pageNum < CommonConstants.DEFAULT_PAGE_NUM) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "pageNum 不能小于 1");
        }
        return pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > CommonConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "pageSize 必须在 1-" + CommonConstants.MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private void cleanupCart(Long userId, List<String> itemIds) {
        if (itemIds.isEmpty()) {
            return;
        }
        RMapCache<String, Object> cart = redissonClient.getMapCache("cart:" + userId);
        cart.fastRemove(itemIds.toArray(String[]::new));
        if (cart.isEmpty()) {
            cart.delete();
        } else {
            cart.expire(CART_TTL);
        }
    }

    private void ensureIdempotencyKeyPresent(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Idempotency-Key 不能为空");
        }
    }

    private Long getIdempotentOrderId(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        if (!bucket.isExists()) {
            return null;
        }
        return Long.valueOf(bucket.get());
    }

    private void saveIdempotentOrderId(String key, Long orderId) {
        redissonClient.getBucket(key).set(String.valueOf(orderId), IDEMPOTENCY_TTL);
    }

    private void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private record NormalizedOrderItem(Long skuId, Integer quantity) {
    }
}
