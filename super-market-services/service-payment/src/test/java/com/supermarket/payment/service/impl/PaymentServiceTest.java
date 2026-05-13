package com.supermarket.payment.service.impl;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.mapper.PaymentIdempotentMapper;
import com.supermarket.payment.mapper.PaymentMapper;
import com.supermarket.payment.mapper.PaymentRefundMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private TestablePaymentServiceImpl paymentService;
    private PaymentMapper paymentMapper;
    private PaymentRefundMapper refundMapper;
    private PaymentIdempotentMapper idempotentMapper;
    private OrderDubboService orderDubboService;

    @BeforeEach
    void setUp() {
        paymentMapper = mock(PaymentMapper.class);
        refundMapper = mock(PaymentRefundMapper.class);
        idempotentMapper = mock(PaymentIdempotentMapper.class);
        orderDubboService = mock(OrderDubboService.class);
        paymentService = new TestablePaymentServiceImpl(refundMapper, idempotentMapper, orderDubboService);
        paymentService.setBaseMapper(paymentMapper);
    }

    @Test
    void shouldCreatePayment() {
        when(paymentMapper.insert(any())).thenReturn(1);
        Payment payment = paymentService.createPayment("ORD001", 1L, new BigDecimal("99.00"), 1);
        assertThat(payment.getPayNo()).startsWith("PAY");
        assertThat(payment.getPayStatus()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicatePayment() {
        when(paymentMapper.insert(any())).thenReturn(1);
        paymentService.createPayment("ORD002", 1L, new BigDecimal("99.00"), 1);

        TestablePaymentServiceImpl spy = spy(paymentService);
        Payment existing = new Payment();
        existing.setPayNo("PAY002");
        existing.setPayStatus(1);
        doReturn(existing).when(spy).getOne(any());

        assertThatThrownBy(() -> spy.createPayment("ORD002", 1L, new BigDecimal("99.00"), 1))
            .hasMessageContaining("已创建支付单");
    }

    @Test
    void shouldHandleCallback() {
        Payment payment = new Payment();
        payment.setPayNo("PAY001");
        payment.setOrderNo("ORD003");
        payment.setPayStatus(1);

        TestablePaymentServiceImpl spy = spy(paymentService);
        doReturn(payment).when(spy).getOne(any());
        when(paymentMapper.updateById(any())).thenReturn(1);
        when(idempotentMapper.getResponseByRequestId(anyString())).thenReturn(null);
        when(idempotentMapper.insert(any())).thenReturn(1);

        spy.handleCallback("req-001", "PAY001", "TPN001");
        assertThat(payment.getPayStatus()).isEqualTo(2);
    }

    @Test
    void shouldBeIdempotentOnRepeatedCallback() {
        TestablePaymentServiceImpl spy = spy(paymentService);
        when(idempotentMapper.getResponseByRequestId("req-002")).thenReturn("{\"status\":\"success\"}");

        String result = spy.handleCallback("req-002", "PAY002", "TPN003");
        assertThat(result).isEqualTo("{\"status\":\"success\"}");
        verify(idempotentMapper, never()).insert(any());
    }

    @Test
    void shouldRefund() {
        Payment payment = new Payment();
        payment.setPayNo("PAY003");
        payment.setOrderNo("ORD005");
        payment.setPayStatus(2);

        TestablePaymentServiceImpl spy = spy(paymentService);
        doReturn(payment).when(spy).getOne(any());
        when(paymentMapper.updateById(any())).thenReturn(1);
        when(refundMapper.insert(any())).thenReturn(1);

        PaymentRefund refund = spy.refund("ORD005", new BigDecimal("99.00"), "不满意");
        assertThat(refund.getRefundNo()).startsWith("RFD");
        assertThat(refund.getRefundStatus()).isEqualTo(2);
    }

    static class TestablePaymentServiceImpl extends PaymentServiceImpl {
        TestablePaymentServiceImpl(PaymentRefundMapper refundMapper, PaymentIdempotentMapper idempotentMapper,
                                    OrderDubboService orderDubboService) {
            super(refundMapper, idempotentMapper, orderDubboService);
        }
        void setBaseMapper(PaymentMapper mapper) {
            this.baseMapper = mapper;
        }
    }
}
