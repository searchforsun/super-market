package com.supermarket.payment.controller;

import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.service.PaymentService;
import com.supermarket.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock the concrete impl class so the mock inherits both PaymentService and
     * PaymentDubboService interfaces, satisfying Dubbo's service export check.
     */
    @MockBean
    private PaymentServiceImpl paymentService;

    @Test
    void shouldCreatePaymentWhenValidParams() throws Exception {
        Payment payment = new Payment();
        payment.setPayNo("PAY202405160001");
        payment.setOrderNo("ORD001");
        payment.setAmount(new BigDecimal("199.99"));
        payment.setPayMethod(1);
        payment.setPayStatus(0);

        when(paymentService.createPaymentEntity("ORD001", 1L, new BigDecimal("199.99"), 1))
                .thenReturn(payment);

        mockMvc.perform(post("/api/payment/pay")
                        .param("orderNo", "ORD001")
                        .param("userId", "1")
                        .param("amount", "199.99")
                        .param("payMethod", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.payNo").value("PAY202405160001"))
                .andExpect(jsonPath("$.data.amount").value(199.99));
    }

    @Test
    void shouldHandleMockCallback() throws Exception {
        when(paymentService.handleCallback(eq("REQ001"), eq("PAY001"), anyString()))
                .thenReturn("SUCCESS");

        mockMvc.perform(post("/api/payment/callback/mock")
                        .param("requestId", "REQ001")
                        .param("payNo", "PAY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("SUCCESS"));
    }

    @Test
    void shouldReturnPaymentWhenQueryByPayNo() throws Exception {
        Payment payment = new Payment();
        payment.setPayNo("PAY001");
        payment.setPayStatus(1);
        payment.setThirdPayNo("TPN123456");

        when(paymentService.getByPayNoEntity("PAY001")).thenReturn(payment);

        mockMvc.perform(get("/api/payment/PAY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.payNo").value("PAY001"))
                .andExpect(jsonPath("$.data.payStatus").value(1));
    }

    @Test
    void shouldRefundWhenValidParams() throws Exception {
        PaymentRefund refund = new PaymentRefund();
        refund.setRefundNo("RF202405160001");
        refund.setOrderNo("ORD001");
        refund.setRefundAmount(new BigDecimal("99.99"));
        refund.setRefundStatus(0);

        when(paymentService.refund("ORD001", new BigDecimal("99.99"), "商品质量问题"))
                .thenReturn(refund);

        mockMvc.perform(post("/api/payment/refund")
                        .param("orderNo", "ORD001")
                        .param("refundAmount", "99.99")
                        .param("reason", "商品质量问题"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.refundNo").value("RF202405160001"))
                .andExpect(jsonPath("$.data.refundStatus").value(0));
    }
}
