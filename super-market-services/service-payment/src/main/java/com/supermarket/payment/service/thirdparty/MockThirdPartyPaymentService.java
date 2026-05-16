package com.supermarket.payment.service.thirdparty;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付 Mock — 开发/测试环境默认生效。
 * <p>
 * 生产切换方式：
 * 1. 实现 {@link ThirdPartyPaymentService}（参考 {@link WechatPayService} / {@link AlipayService}）
 * 2. 在实现类上加 @Service 并移除本类的 @ConditionalOnMissingBean，或通过 profile 切换
 * <p>
 * 必须对接：微信支付或支付宝
 */
@Slf4j
@Service
@ConditionalOnMissingBean(value = ThirdPartyPaymentService.class, ignored = MockThirdPartyPaymentService.class)
public class MockThirdPartyPaymentService implements ThirdPartyPaymentService {

    @Override
    public String prepay(String payNo, String orderNo, BigDecimal amount, String subject) {
        String mockPrepayId = "MOCK_PREPAY_" + IdUtil.getSnowflakeNextId();
        log.info("[支付Mock] prepay: payNo={}, orderNo={}, amount={}, subject={} → mockId={}",
                payNo, orderNo, amount, subject, mockPrepayId);
        return mockPrepayId;
    }

    @Override
    public boolean verifySignature(String rawBody) {
        log.info("[支付Mock] 回调验签始终返回 true, body preview: {}",
                rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody);
        return true;
    }

    @Override
    public String refund(String payNo, String refundNo, BigDecimal totalAmount, BigDecimal refundAmount, String reason) {
        String mockRefundId = "MOCK_REFUND_" + IdUtil.getSnowflakeNextId();
        log.info("[支付Mock] refund: payNo={}, refundNo={}, total={}, refund={}, reason={} → mockId={}",
                payNo, refundNo, totalAmount, refundAmount, reason, mockRefundId);
        return mockRefundId;
    }
}
