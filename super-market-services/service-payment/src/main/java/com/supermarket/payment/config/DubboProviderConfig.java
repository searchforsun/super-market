package com.supermarket.payment.config;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@DubboComponentScan(basePackages = "com.supermarket.payment")
public class DubboProviderConfig {
}
