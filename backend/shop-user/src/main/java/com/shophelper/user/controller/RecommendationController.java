package com.shophelper.user.controller;

import com.shophelper.common.core.constant.CommonConstants;
import com.shophelper.common.core.result.Result;
import com.shophelper.user.dto.RecommendationResponse;
import com.shophelper.user.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户推荐接口
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/me")
    public Result<RecommendationResponse> getMyRecommendations(@RequestParam("scene") String scene,
                                                               @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                                               HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute(CommonConstants.ATTR_CURRENT_USER_ID)));
        return Result.success(recommendationService.getMyRecommendations(userId, scene, pageSize))
                .requestId((String) request.getAttribute("requestId"));
    }
}
