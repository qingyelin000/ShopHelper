package com.shophelper.product.config;

import com.shophelper.product.security.ProductAdminAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 商品服务 Web 配置
 */
@Configuration
@RequiredArgsConstructor
public class ProductWebMvcConfig implements WebMvcConfigurer {

    private final ProductAdminAuthInterceptor productAdminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(productAdminAuthInterceptor)
                .addPathPatterns("/api/v1/products/admin/**");
    }
}
