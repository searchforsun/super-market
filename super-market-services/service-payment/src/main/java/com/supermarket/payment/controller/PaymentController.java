package com.supermarket.payment.controller;

import cn.hutool.core.util.IdUtil;
import com.supermarket.common.core.result.R;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付服务", description = "支付创建、回调、查询、退款接口")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    @Operation(summary = "创建支付")
    public R<Payment> pay(@Parameter(description = "订单号") @RequestParam @NotBlank(message = "订单号不能为空") String orderNo,
                          @Parameter(description = "用户ID") @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
                          @Parameter(description = "支付金额") @RequestParam @NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于0") BigDecimal amount,
                          @Parameter(description = "支付方式") @RequestParam(defaultValue = "1") Integer payMethod) {
        return R.ok(paymentService.createPaymentEntity(orderNo, userId, amount, payMethod));
    }

    @PostMapping("/callback/mock")
    @Operation(summary = "模拟支付回调")
    public R<String> mockCallback(@Parameter(description = "请求ID") @RequestParam String requestId,
                                  @Parameter(description = "支付流水号") @RequestParam String payNo) {
        String thirdPayNo = "TPN" + IdUtil.getSnowflakeNextId();
        return R.ok(paymentService.handleCallback(requestId, payNo, thirdPayNo));
    }

    @GetMapping("/{payNo}")
    @Operation(summary = "查询支付记录")
    public R<Payment> query(@Parameter(description = "支付流水号") @PathVariable String payNo) {
        return R.ok(paymentService.getByPayNoEntity(payNo));
    }

    @PostMapping("/refund")
    @Operation(summary = "申请退款")
    public R<PaymentRefund> refund(@Parameter(description = "订单号") @RequestParam @NotBlank(message = "订单号不能为空") String orderNo,
                                   @Parameter(description = "退款金额") @RequestParam @NotNull(message = "退款金额不能为空") @DecimalMin(value = "0.01", message = "退款金额必须大于0") BigDecimal refundAmount,
                                   @Parameter(description = "退款原因") @RequestParam @Size(max = 500, message = "退款原因最长500字") String reason) {
        return R.ok(paymentService.refund(orderNo, refundAmount, reason));
    }
}
