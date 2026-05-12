package com.supermarket.review.config;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@DubboComponentScan(basePackages = "com.supermarket.review")
public class DubboProviderConfig {
}
