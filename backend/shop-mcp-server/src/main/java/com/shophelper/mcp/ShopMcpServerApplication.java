package com.shophelper.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.shophelper")
@EnableDiscoveryClient
@EnableFeignClients
public class ShopMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopMcpServerApplication.class, args);
    }
}
