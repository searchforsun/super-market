package com.supermarket.common.core.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode implements IResultCode {

    // === SYSTEM 90001~90099 ===
    SUCCESS(0, "success"),
    SYSTEM_ERROR(90001, "系统繁忙，请稍后重试"),
    PARAM_ERROR(90002, "参数错误"),
    UNAUTHORIZED(90003, "未授权"),
    FORBIDDEN(90004, "权限不足"),
    NOT_FOUND(90005, "资源不存在"),

    // === USER 10001~10099 ===
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PHONE_EXISTS(10002, "手机号已注册"),
    USER_PASSWORD_ERROR(10003, "密码错误"),
    USER_TOKEN_EXPIRED(10004, "Token已过期"),
    USER_TOKEN_INVALID(10005, "Token无效"),

    // === PRODUCT 20001~20099 ===
    PRODUCT_NOT_FOUND(20001, "商品不存在"),
    PRODUCT_AUDIT_FAILED(20002, "商品审核失败"),
    CATEGORY_NOT_FOUND(20003, "类目不存在"),
    CATEGORY_HAS_CHILDREN(20004, "类目下有子类目，无法删除"),
    SKU_NOT_FOUND(20005, "SKU不存在"),
    STOCK_INSUFFICIENT(20006, "库存不足"),

    // === ORDER 30001~30099 ===
    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_STATUS_ERROR(30002, "订单状态不正确"),
    ORDER_CANNOT_CANCEL(30003, "仅待付款订单可取消"),
    ORDER_CANNOT_SHIP(30004, "仅待发货订单可发货"),
    PAYMENT_DUPLICATE(30005, "该订单已创建支付单"),
    PAYMENT_NOT_FOUND(30006, "支付单不存在"),
    PAYMENT_STATUS_ERROR(30007, "支付单状态不正确"),
    CART_ITEM_NOT_FOUND(30008, "购物车商品不存在"),

    // === MARKETING 40001~40099 ===
    COUPON_TEMPLATE_NOT_FOUND(40001, "优惠券模板不存在"),
    COUPON_STOCK_INSUFFICIENT(40002, "优惠券库存不足"),
    COUPON_CLAIM_LIMIT(40003, "已达每人领取上限"),
    SECKILL_SESSION_NOT_FOUND(40004, "秒杀场次不存在"),
    SECKILL_STOCK_INSUFFICIENT(40005, "秒杀库存不足"),
    SECKILL_LIMIT_REACHED(40006, "已达每人限购数量"),

    // === SHOP 50001~50099 ===
    MERCHANT_NOT_FOUND(50001, "商家不存在"),
    MERCHANT_AUDIT_FAILED(50002, "商家审核失败"),
    SHOP_NOT_FOUND(50003, "店铺不存在"),

    // === PLATFORM 60001~60099 ===
    BANNER_NOT_FOUND(60001, "Banner不存在"),
    RISK_RULE_NOT_FOUND(60002, "风控规则不存在"),

    // === FILE 70001~70099 ===
    FILE_UPLOAD_FAILED(70001, "文件上传失败"),
    FILE_NOT_FOUND(70002, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(70003, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(70004, "文件大小超出限制"),
    NOTIFY_TEMPLATE_NOT_FOUND(70005, "通知模板不存在"),
    NOTIFY_SEND_FAILED(70006, "通知发送失败");

    private final int code;
    private final String message;
}
