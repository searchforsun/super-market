package com.supermarket.payment.service.thirdparty;

import java.math.BigDecimal;

/**
 * 第三方支付渠道统一接口。
 * <p>
 * 生产环境对接：微信支付 ({@link WechatPayService}) / 支付宝 ({@link AlipayService})
 * 开发环境使用：{@link MockThirdPartyPaymentService}
 */
public interface ThirdPartyPaymentService {

    /**
     * 预付单 — 向第三方发起支付请求，返回支付链接/二维码
     *
     * @param payNo     内部支付流水号
     * @param orderNo   订单号
     * @param amount    金额（元）
     * @param subject   商品描述
     * @return 第三方支付串（微信 prepay_id / 支付宝 trade_no）
     */
    String prepay(String payNo, String orderNo, BigDecimal amount, String subject);

    /**
     * 支付回调验签
     *
     * @param rawBody 回调原始报文
     * @return true 签名通过
     */
    boolean verifySignature(String rawBody);

    /**
     * 退款
     *
     * @param payNo        内部支付流水号
     * @param refundNo     内部退款流水号
     * @param totalAmount  原支付金额
     * @param refundAmount 退款金额
     * @param reason       退款原因
     * @return 第三方退款单号
     */
    String refund(String payNo, String refundNo, BigDecimal totalAmount, BigDecimal refundAmount, String reason);
}
