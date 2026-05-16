package com.supermarket.payment.service.thirdparty;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 微信支付接入（待实现）。
 * <p>
 * 对接清单：
 * <ol>
 *   <li>引入 wechatpay-java SDK（com.github.wechatpay-apiv3:wechatpay-java）</li>
 *   <li>配置商户号 mchid、APIv3 密钥、证书序列号、私钥路径</li>
 *   <li>实现 JSAPI / Native / H5 下单 → 返回 prepay_id</li>
 *   <li>实现回调验签（AES-256-GCM 解密 + 签名验证）</li>
 *   <li>实现退款（按退款单号）</li>
 * </ol>
 * <p>
 * 配置项示例（application.yml）：
 * <pre>
 * wechat:
 *   pay:
 *     mch-id: 1234567890
 *     api-v3-key: ${WECHAT_API_V3_KEY}
 *     serial-no: ${WECHAT_SERIAL_NO}
 *     private-key-path: /etc/secrets/wechat-apiclient_key.pem
 *     notify-url: https://your-domain/api/payment/callback/wechat
 * </pre>
 * <p>
 * 使用时取消 @Slf4j 下方注释，注入 WechatPayClient 后实现三个方法。
 *
 * @see <a href="https://pay.weixin.qq.com/docs/merchant/development/overview.html">微信支付开发文档</a>
 */
@Slf4j
// @Service  // 对接完成后取消注释，同时移除 MockThirdPartyPaymentService 的 @ConditionalOnMissingBean
public class WechatPayService implements ThirdPartyPaymentService {

    // @Resource
    // private com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension jsapiService;

    @Override
    public String prepay(String payNo, String orderNo, BigDecimal amount, String subject) {
        // TODO 对接微信支付：
        //   PrepayRequest request = new PrepayRequest();
        //   request.setAppid(appId);
        //   request.setMchid(mchId);
        //   request.setOutTradeNo(payNo);
        //   request.setDescription(subject);
        //   request.setNotifyUrl(notifyUrl);
        //   Amount amt = new Amount();
        //   amt.setTotal(amount.multiply(new BigDecimal("100")).intValue()); // 分
        //   amt.setCurrency("CNY");
        //   request.setAmount(amt);
        //   Payer payer = new Payer();
        //   payer.setOpenid(openid);
        //   request.setPayer(payer);
        //   PrepayResponse response = jsapiService.prepay(request);
        //   return response.getPrepayId();
        throw new UnsupportedOperationException("微信支付尚未对接，开发环境请使用 Mock");
    }

    @Override
    public boolean verifySignature(String rawBody) {
        // TODO 微信回调验签：NotificationParser.parse() → 验签 + 解密
        throw new UnsupportedOperationException("微信支付尚未对接");
    }

    @Override
    public String refund(String payNo, String refundNo, BigDecimal totalAmount, BigDecimal refundAmount, String reason) {
        // TODO 微信退款：jsapiService.refund(...)
        throw new UnsupportedOperationException("微信支付尚未对接");
    }
}
