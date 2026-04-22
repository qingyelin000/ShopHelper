package com.shophelper.cart.controller;

import com.shophelper.cart.dto.AddCartItemRequest;
import com.shophelper.cart.dto.CartSnapshotResponse;
import com.shophelper.cart.dto.UpdateCartItemRequest;
import com.shophelper.cart.service.CartService;
import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.Result;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<CartSnapshotResponse> getCart(HttpServletRequest request) {
        return Result.success(cartService.getCart(getCurrentUserId(request)))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PostMapping("/items")
    public Result<CartSnapshotResponse> addItem(@Valid @RequestBody AddCartItemRequest requestBody,
                                                HttpServletRequest request) {
        return Result.success(cartService.addItem(getCurrentUserId(request), requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @PutMapping("/items/{itemId}")
    public Result<CartSnapshotResponse> updateItem(@PathVariable String itemId,
                                                   @Valid @RequestBody UpdateCartItemRequest requestBody,
                                                   HttpServletRequest request) {
        return Result.success(cartService.updateItem(getCurrentUserId(request), itemId, requestBody))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<CartSnapshotResponse> removeItem(@PathVariable String itemId, HttpServletRequest request) {
        return Result.success(cartService.removeItem(getCurrentUserId(request), itemId))
                .requestId((String) request.getAttribute("requestId"));
    }

    @DeleteMapping
    public Result<CartSnapshotResponse> clear(HttpServletRequest request) {
        return Result.success(cartService.clear(getCurrentUserId(request)))
                .requestId((String) request.getAttribute("requestId"));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return Long.valueOf(String.valueOf(request.getAttribute(CommonConstants.ATTR_CURRENT_USER_ID)));
    }
}
