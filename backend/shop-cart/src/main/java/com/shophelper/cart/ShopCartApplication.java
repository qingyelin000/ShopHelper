package com.shophelper.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.shophelper")
@EnableDiscoveryClient
@EnableFeignClients
public class ShopCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopCartApplication.class, args);
    }
}
