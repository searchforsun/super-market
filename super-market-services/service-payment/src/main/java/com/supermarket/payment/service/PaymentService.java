package com.supermarket.payment.service;

import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;

import java.math.BigDecimal;

public interface PaymentService {
    Payment createPaymentEntity(String orderNo, Long userId, BigDecimal amount, Integer payMethod);
    Payment getByPayNoEntity(String payNo);
    Payment getByOrderNo(String orderNo);
    String handleCallback(String requestId, String payNo, String thirdPayNo);
    PaymentRefund refund(String orderNo, BigDecimal refundAmount, String reason);
}
