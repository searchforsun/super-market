package com.supermarket.address;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "com.supermarket.address",
    "com.supermarket.common"
})
public class AddressApplication {
    public static void main(String[] args) {
        SpringApplication.run(AddressApplication.class, args);
    }
}
