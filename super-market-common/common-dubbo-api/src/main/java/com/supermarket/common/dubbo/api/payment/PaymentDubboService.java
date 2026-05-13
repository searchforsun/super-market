package com.supermarket.common.dubbo.api.payment;

import com.supermarket.common.dubbo.api.payment.dto.PaymentDTO;

public interface PaymentDubboService {
    PaymentDTO createPayment(String orderNo, Long userId, java.math.BigDecimal amount, Integer payMethod);
    PaymentDTO getByPayNo(String payNo);
    void handleCallback(String payNo, String thirdPayNo);
}
