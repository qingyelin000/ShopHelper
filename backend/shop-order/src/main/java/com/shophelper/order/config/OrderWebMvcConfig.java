package com.shophelper.order.config;

import com.shophelper.order.security.OrderAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 订单服务 Web 配置
 */
@Configuration
@RequiredArgsConstructor
public class OrderWebMvcConfig implements WebMvcConfigurer {

    private final OrderAuthInterceptor orderAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orderAuthInterceptor)
                .addPathPatterns("/api/v1/orders/**");
    }
}
