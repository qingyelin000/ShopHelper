package com.shophelper.cart.config;

import com.shophelper.cart.security.CartAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 购物车服务 Web 配置
 */
@Configuration
@RequiredArgsConstructor
public class CartWebMvcConfig implements WebMvcConfigurer {

    private final CartAuthInterceptor cartAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cartAuthInterceptor)
                .addPathPatterns("/api/v1/cart/**");
    }
}
