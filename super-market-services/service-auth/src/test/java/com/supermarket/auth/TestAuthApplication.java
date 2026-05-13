package com.supermarket.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.supermarket.auth",
    "com.supermarket.common"
})
public class TestAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestAuthApplication.class, args);
    }
}
