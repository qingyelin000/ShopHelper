package com.shophelper.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.shophelper")
@EnableDiscoveryClient
public class ShopSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSeckillApplication.class, args);
    }
}
