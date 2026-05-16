package com.supermarket.notify.service.email;

/**
 * 邮件发送统一接口。
 * <p>
 * 生产环境对接：阿里云邮件推送 ({@link AliyunEmailService}) / 腾讯云邮件 ({@link TencentCloudEmailService})
 * 开发环境使用：{@link MockEmailService}
 */
public interface EmailService {

    /**
     * 发送单封邮件
     *
     * @param to      收件人邮箱
     * @param subject 主题
     * @param body    正文（HTML）
     */
    void send(String to, String subject, String body);

    /**
     * 批量发送
     *
     * @param to      收件人邮箱列表
     * @param subject 主题
     * @param body    正文（HTML）
     */
    void sendBatch(String[] to, String subject, String body);
}
