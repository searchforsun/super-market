package com.supermarket.payment.service.impl;

import com.supermarket.common.dubbo.api.payment.PaymentDubboService;
import com.supermarket.common.dubbo.api.payment.dto.PaymentDTO;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.service.PaymentService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@DubboService
public class PaymentDubboServiceImpl implements PaymentDubboService {

    @Autowired
    private PaymentService paymentService;

    @Override
    public PaymentDTO createPayment(String orderNo, Long userId, BigDecimal amount, Integer payMethod) {
        Payment p = paymentService.createPayment(orderNo, userId, amount, payMethod);
        return toDTO(p);
    }

    @Override
    public PaymentDTO getByPayNo(String payNo) {
        Payment p = paymentService.getByPayNo(payNo);
        return toDTO(p);
    }

    @Override
    public void handleCallback(String payNo, String thirdPayNo) {
        paymentService.handleCallback("DUBBO_" + payNo, payNo, thirdPayNo);
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
