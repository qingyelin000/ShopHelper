package com.shophelper.order.controller;

import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.PageResult;
import com.shophelper.common.core.result.Result;
import com.shophelper.order.dto.CancelOrderRequest;
import com.shophelper.order.dto.CancelOrderResponse;
import com.shophelper.order.dto.CreateOrderRequest;
import com.shophelper.order.dto.CreateOrderResponse;
import com.shophelper.order.dto.OrderDetailResponse;
import com.shophelper.order.dto.OrderSummaryResponse;
import com.shophelper.order.dto.PayOrderRequest;
import com.shophelper.order.dto.PayOrderResponse;
import com.shophelper.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest requestBody,
                                                   @RequestHeader(name = CommonConstants.HEADER_IDEMPOTENCY_KEY, required = false)
                                                   String idempotencyKey,
                                                   HttpServletRequest request) {
        return Result.success(orderService.createOrder(getCurrentUserId(request), requestBody, idempotencyKey))
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping
    public Result<PageResult<OrderSummaryResponse>> listOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {
        return Result.success(orderService.listOrders(getCurrentUserId(request), status, pageNum, pageSize))
                .requestId((String) request.getAttribute("requestId"));
    }

    @GetMapping("/{orderId}")
    public Result<OrderDetailResponse> getOrder(@PathVariable Long orderId, HttpServletRequest request) {
        return Result.success(orderService.getOrder(getCurrentUserId(request), orderId))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<CancelOrderResponse> cancelOrder(@PathVariable Long orderId,
                                                   @Valid @RequestBody CancelOrderRequest requestBody,
                                                   HttpServletRequest request) {
        return Result.success(orderService.cancelOrder(getCurrentUserId(request), orderId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/{orderId}/pay")
    public Result<PayOrderResponse> payOrder(@PathVariable Long orderId,
                                             @Valid @RequestBody PayOrderRequest requestBody,
                                             @RequestHeader(name = CommonConstants.HEADER_IDEMPOTENCY_KEY, required = false)
                                             String idempotencyKey,
                                             HttpServletRequest request) {
        return Result.success(orderService.payOrder(getCurrentUserId(request), orderId, requestBody, idempotencyKey))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirmOrder(@PathVariable Long orderId, HttpServletRequest request) {
        orderService.confirmOrder(getCurrentUserId(request), orderId);
        return Result.<Void>success()
                .requestId((String) request.getAttribute("requestId"));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return Long.valueOf(String.valueOf(request.getAttribute(CommonConstants.ATTR_CURRENT_USER_ID)));
    }
}
