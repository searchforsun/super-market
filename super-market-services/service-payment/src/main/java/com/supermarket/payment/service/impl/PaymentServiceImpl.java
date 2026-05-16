package com.supermarket.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.payment.PaymentDubboService;
import com.supermarket.common.dubbo.api.payment.dto.PaymentDTO;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentIdempotent;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.mapper.PaymentIdempotentMapper;
import com.supermarket.payment.mapper.PaymentMapper;
import com.supermarket.payment.mapper.PaymentRefundMapper;
import com.supermarket.payment.service.PaymentService;
import com.supermarket.payment.service.thirdparty.ThirdPartyPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@DubboService(interfaceClass = PaymentDubboService.class)
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService, PaymentDubboService {

    private final PaymentRefundMapper refundMapper;
    private final PaymentIdempotentMapper idempotentMapper;

    @DubboReference(check = false)
    private OrderDubboService orderDubboService;

    /**
     * 生产环境替换为 WechatPayService 或 AlipayService
     * 开发环境自动注入 {@link com.supermarket.payment.service.thirdparty.MockThirdPartyPaymentService}
     */
    @Autowired
    private ThirdPartyPaymentService thirdPartyPaymentService;

    @Autowired
    public PaymentServiceImpl(PaymentRefundMapper refundMapper, PaymentIdempotentMapper idempotentMapper) {
        this.refundMapper = refundMapper;
        this.idempotentMapper = idempotentMapper;
    }

    public PaymentServiceImpl(PaymentRefundMapper refundMapper, PaymentIdempotentMapper idempotentMapper,
                               OrderDubboService orderDubboService) {
        this.refundMapper = refundMapper;
        this.idempotentMapper = idempotentMapper;
        this.orderDubboService = orderDubboService;
    }

    // -- PaymentService impl (local entity) --

    @Override
    @Transactional
    public Payment createPaymentEntity(String orderNo, Long userId, BigDecimal amount, Integer payMethod) {
        Payment exist = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        if (exist != null) throw new BizException(400, "该订单已创建支付单");

        String payNo = "PAY" + IdUtil.getSnowflakeNextId();
        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayMethod(payMethod);
        payment.setPayStatus(1);
        save(payment);
        return payment;
    }

    @Override
    public Payment getByPayNoEntity(String payNo) {
        Payment p = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getPayNo, payNo));
        if (p == null) throw new BizException(404, "支付单不存在");
        return p;
    }

    @Override
    public Payment getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
    }

    @Override
    @Transactional
    public String handleCallback(String requestId, String payNo, String thirdPayNo) {
        String cached = idempotentMapper.getResponseByRequestId(requestId);
        if (cached != null) {
            log.info("重复回调: requestId={}", requestId);
            return cached;
        }

        Payment payment = getByPayNoEntity(payNo);
        if (payment.getPayStatus() != 1) {
            throw new BizException(400, "支付单状态不正确");
        }

        payment.setPayStatus(2);
        payment.setThirdPayNo(thirdPayNo);
        payment.setPaidAt(LocalDateTime.now());
        updateById(payment);

        orderDubboService.updateStatus(payment.getOrderNo(), 2);

        PaymentIdempotent idem = new PaymentIdempotent();
        idem.setRequestId(requestId);
        idem.setBusinessType("PAYMENT");
        idem.setBusinessNo(payNo);
        idem.setResponseData("{\"status\":\"success\"}");
        idem.setCreatedAt(LocalDateTime.now());
        idempotentMapper.insert(idem);

        return "success";
    }

    @Override
    @Transactional
    public PaymentRefund refund(String orderNo, BigDecimal refundAmount, String reason) {
        Payment payment = getByOrderNo(orderNo);
        if (payment == null || payment.getPayStatus() != 2) {
            throw new BizException(400, "仅已支付订单可退款");
        }

        String refundNo = "RFD" + IdUtil.getSnowflakeNextId();

        // 调用第三方支付退款（开发环境走 Mock）
        thirdPartyPaymentService.refund(payment.getPayNo(), refundNo,
                payment.getAmount(), refundAmount, reason);

        PaymentRefund refund = new PaymentRefund();
        refund.setRefundNo(refundNo);
        refund.setPayNo(payment.getPayNo());
        refund.setOrderNo(orderNo);
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(reason);
        refund.setRefundStatus(2);
        refund.setRefundedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        payment.setPayStatus(4);
        updateById(payment);

        orderDubboService.updateStatus(orderNo, 6);

        return refund;
    }

    // -- PaymentDubboService impl (Dubbo RPC) --

    @Override
    public PaymentDTO createPayment(String orderNo, Long userId, BigDecimal amount, Integer payMethod) {
        Payment p = createPaymentEntity(orderNo, userId, amount, payMethod);
        return toDTO(p);
    }

    @Override
    public PaymentDTO getByPayNo(String payNo) {
        Payment p = getByPayNoEntity(payNo);
        return toDTO(p);
    }

    @Override
    public void handleCallback(String payNo, String thirdPayNo) {
        String requestId = "DUBBO_" + payNo + "_" + System.currentTimeMillis();
        handleCallback(requestId, payNo, thirdPayNo);
    }

    private PaymentDTO toDTO(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPayNo(p.getPayNo());
        dto.setOrderNo(p.getOrderNo());
        dto.setAmount(p.getAmount());
        dto.setPayMethod(p.getPayMethod());
        dto.setPayStatus(p.getPayStatus());
        dto.setThirdPayNo(p.getThirdPayNo());
        dto.setPaidAt(p.getPaidAt());
        return dto;
    }
}
