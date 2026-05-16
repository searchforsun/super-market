package com.supermarket.notify.service.email;

import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云邮件推送（DirectMail / DM）接入（待实现）。
 * <p>
 * 对接清单：
 * <ol>
 *   <li>引入 aliyun-sdk-dm（com.aliyun:aliyun-java-sdk-dm）</li>
 *   <li>配置 AccessKey、发信域名、发信地址</li>
 *   <li>SingleSendMail / BatchSendMail</li>
 * </ol>
 * <p>
 * 配置项示例（application.yml）：
 * <pre>
 * aliyun:
 *   email:
 *     access-key-id: ${ALIYUN_ACCESS_KEY_ID}
 *     access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
 *     account-name: noreply@mail.your-domain.com
 *     from-alias: 超级商城
 *     region: cn-hangzhou
 * </pre>
 * <p>
 * 使用时取消 @Slf4j 下方注释，注入 IAcsClient 后实现 send/sendBatch。
 *
 * @see <a href="https://help.aliyun.com/document_detail/29444.html">阿里云邮件推送文档</a>
 */
@Slf4j
// @Service  // 对接完成后取消注释，同时移除 MockEmailService 的 @ConditionalOnMissingBean
public class AliyunEmailService implements EmailService {

    // @Resource
    // private com.aliyuncs.IAcsClient acsClient;
    // @Value("${aliyun.email.account-name}")
    // private String accountName;
    // @Value("${aliyun.email.from-alias}")
    // private String fromAlias;

    @Override
    public void send(String to, String subject, String body) {
        // TODO 对接阿里云邮件推送：
        //   SingleSendMailRequest request = new SingleSendMailRequest();
        //   request.setAccountName(accountName);
        //   request.setFromAlias(fromAlias);
        //   request.setAddressType(1);
        //   request.setReplyToAddress(false);
        //   request.setToAddress(to);
        //   request.setSubject(subject);
        //   request.setHtmlBody(body);
        //   acsClient.getAcsResponse(request);
        throw new UnsupportedOperationException("阿里云邮件尚未对接，开发环境请使用 Mock");
    }

    @Override
    public void sendBatch(String[] to, String subject, String body) {
        // TODO 批量邮件：BatchSendMailRequest 或 循环调用 send
        throw new UnsupportedOperationException("阿里云邮件尚未对接");
    }
}
