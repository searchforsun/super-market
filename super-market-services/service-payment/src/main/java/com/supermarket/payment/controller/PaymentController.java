package com.supermarket.payment.controller;

import cn.hutool.core.util.IdUtil;
import com.supermarket.common.core.result.R;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public R<Payment> pay(@RequestParam String orderNo,
                          @RequestParam Long userId,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "1") Integer payMethod) {
        return R.ok(paymentService.createPayment(orderNo, userId, amount, payMethod));
    }

    @PostMapping("/callback/mock")
    public R<String> mockCallback(@RequestParam String requestId,
                                  @RequestParam String payNo) {
        String thirdPayNo = "TPN" + IdUtil.getSnowflakeNextId();
        return R.ok(paymentService.handleCallback(requestId, payNo, thirdPayNo));
    }

    @GetMapping("/{payNo}")
    public R<Payment> query(@PathVariable String payNo) {
        return R.ok(paymentService.getByPayNo(payNo));
    }

    @PostMapping("/refund")
    public R<PaymentRefund> refund(@RequestParam String orderNo,
                                   @RequestParam BigDecimal refundAmount,
                                   @RequestParam String reason) {
        return R.ok(paymentService.refund(orderNo, refundAmount, reason));
    }
}
