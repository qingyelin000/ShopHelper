package com.shophelper.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.shophelper")
@EnableDiscoveryClient
public class ShopSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSearchApplication.class, args);
    }
}
