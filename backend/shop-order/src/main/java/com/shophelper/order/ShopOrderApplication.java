package com.shophelper.order;

import com.shophelper.order.config.OrderJwtProperties;
import com.shophelper.order.config.OrderUserSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.shophelper")
@EnableDiscoveryClient
@EnableConfigurationProperties({OrderJwtProperties.class, OrderUserSecurityProperties.class})
public class ShopOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopOrderApplication.class, args);
    }
}
