package com.supermarket.notify.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 邮件 Mock — 开发/测试环境默认生效，仅打印日志不实际发送。
 * <p>
 * 生产切换方式：
 * 1. 实现 {@link EmailService}（参考 {@link AliyunEmailService} / {@link TencentCloudEmailService}）
 * 2. 在实现类上加 @Service 并移除本类的 @ConditionalOnMissingBean，或通过 profile 切换
 * <p>
 * 必须对接：阿里云邮件推送 或 腾讯云邮件
 */
@Slf4j
@Service
@ConditionalOnMissingBean(value = EmailService.class, ignored = MockEmailService.class)
public class MockEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[邮件Mock] TO: {} | SUBJECT: {} | BODY({} chars): {}",
                to, subject, body.length(),
                body.length() > 300 ? body.substring(0, 300) + "..." : body);
    }

    @Override
    public void sendBatch(String[] to, String subject, String body) {
        log.info("[邮件Mock] BATCH TO: {} 人({}) | SUBJECT: {} | BODY({} chars): {}",
                to.length, String.join(",", to), subject, body.length(),
                body.length() > 300 ? body.substring(0, 300) + "..." : body);
    }
}
