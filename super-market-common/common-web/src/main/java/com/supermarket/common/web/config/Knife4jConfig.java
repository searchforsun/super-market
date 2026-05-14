package com.supermarket.common.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Super Market API 文档")
                .description("京东式全栈电商平台 — 18 个微服务 API 接口文档")
                .version("1.0.0")
                .contact(new Contact().name("sunachao")));
    }
}
