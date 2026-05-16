package com.supermarket.payment.service.thirdparty;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 支付宝接入（待实现）。
 * <p>
 * 对接清单：
 * <ol>
 *   <li>引入 alipay-sdk-java（com.alipay.sdk:alipay-sdk-java）</li>
 *   <li>配置 appId、商户私钥、支付宝公钥、网关地址</li>
 *   <li>实现 alipay.trade.create / alipay.trade.pay → 返回 tradeNo</li>
 *   <li>实现回调验签（RSA2 签名验证）</li>
 *   <li>实现 alipay.trade.refund</li>
 * </ol>
 * <p>
 * 配置项示例（application.yml）：
 * <pre>
 * alipay:
 *   app-id: 2021000000000000
 *   private-key: ${ALIPAY_PRIVATE_KEY}
 *   alipay-public-key: ${ALIPAY_PUBLIC_KEY}
 *   gateway: https://openapi.alipay.com/gateway.do
 *   notify-url: https://your-domain/api/payment/callback/alipay
 * </pre>
 * <p>
 * 使用时取消 @Slf4j 下方注释，注入 AlipayClient 后实现三个方法。
 *
 * @see <a href="https://opendocs.alipay.com/open/194">支付宝支付文档</a>
 */
@Slf4j
// @Service  // 对接完成后取消注释，同时移除 MockThirdPartyPaymentService 的 @ConditionalOnMissingBean
public class AlipayService implements ThirdPartyPaymentService {

    // @Resource
    // private com.alipay.api.AlipayClient alipayClient;

    @Override
    public String prepay(String payNo, String orderNo, BigDecimal amount, String subject) {
        // TODO 对接支付宝：
        //   AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
        //   JSONObject biz = new JSONObject();
        //   biz.put("out_trade_no", payNo);
        //   biz.put("total_amount", amount);
        //   biz.put("subject", subject);
        //   request.setBizContent(biz.toString());
        //   request.setNotifyUrl(notifyUrl);
        //   AlipayTradeCreateResponse response = alipayClient.certificateExecute(request);
        //   return response.getTradeNo();
        throw new UnsupportedOperationException("支付宝尚未对接，开发环境请使用 Mock");
    }

    @Override
    public boolean verifySignature(String rawBody) {
        // TODO 支付宝回调验签：AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2")
        throw new UnsupportedOperationException("支付宝尚未对接");
    }

    @Override
    public String refund(String payNo, String refundNo, BigDecimal totalAmount, BigDecimal refundAmount, String reason) {
        // TODO 支付宝退款：alipayClient.execute(new AlipayTradeRefundRequest())
        throw new UnsupportedOperationException("支付宝尚未对接");
    }
}
