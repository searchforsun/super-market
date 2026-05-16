package com.supermarket.notify.service.email;

import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云邮件（SES）接入（待实现）。
 * <p>
 * 对接清单：
 * <ol>
 *   <li>引入 tencentcloud-sdk-java-ses（com.tencentcloudapi:tencentcloud-sdk-java-ses）</li>
 *   <li>配置 SecretId、SecretKey、发信域名</li>
 *   <li>SendEmail</li>
 * </ol>
 * <p>
 * 配置项示例（application.yml）：
 * <pre>
 * tencent:
 *   cloud:
 *     secret-id: ${TENCENT_SECRET_ID}
 *     secret-key: ${TENCENT_SECRET_KEY}
 *     email:
 *       from-address: noreply@mail.your-domain.com
 *       region: ap-guangzhou
 * </pre>
 * <p>
 * 使用时取消 @Slf4j 下方注释，注入 SesClient 后实现 send/sendBatch。
 *
 * @see <a href="https://cloud.tencent.com/document/product/1288">腾讯云邮件推送文档</a>
 */
@Slf4j
// @Service  // 对接完成后取消注释，同时移除 MockEmailService 的 @ConditionalOnMissingBean
public class TencentCloudEmailService implements EmailService {

    // @Resource
    // private com.tencentcloudapi.ses.v20201002.SesClient sesClient;
    // @Value("${tencent.cloud.email.from-address}")
    // private String fromAddress;

    @Override
    public void send(String to, String subject, String body) {
        // TODO 对接腾讯云邮件：
        //   SendEmailRequest req = new SendEmailRequest();
        //   req.setFromEmailAddress(fromAddress);
        //   req.setDestination(new String[]{to});
        //   Template template = new Template();
        //   template.setTemplateData("{\"subject\":\"" + subject + "\",\"body\":\"" + body + "\"}");
        //   req.setTemplate(template);
        //   req.setSubject(subject);
        //   Simple simple = new Simple();
        //   simple.setHtml(body);
        //   req.setSimple(simple);
        //   sesClient.SendEmail(req);
        throw new UnsupportedOperationException("腾讯云邮件尚未对接，开发环境请使用 Mock");
    }

    @Override
    public void sendBatch(String[] to, String subject, String body) {
        // TODO 批量邮件：SendEmailRequest.setDestination(to) 或 BatchSendEmail
        throw new UnsupportedOperationException("腾讯云邮件尚未对接");
    }
}
