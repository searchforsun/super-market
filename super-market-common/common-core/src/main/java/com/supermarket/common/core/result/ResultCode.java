package com.supermarket.common.core.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 全局错误码枚举
 * 区间划分：
 * - 成功: 0
 * - 系统通用: 90000~90999
 * - 用户模块: 10000~10999
 * - 商品模块: 20000~20999
 * - 订单模块: 30000~30999
 * - 营销模块: 40000~40999
 * - 商家模块: 50000~50999
 * - 平台模块: 60000~60999
 * - 文件模块: 70000~70999
 * - 评价模块: 80000~80999
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode implements IResultCode {

    // ==================== 成功 ====================
    SUCCESS(0, "success", ErrorType.SUCCESS),

    // ==================== 系统通用 90000~90999 ====================
    SYSTEM_ERROR(90001, "系统繁忙，请稍后重试", ErrorType.SYSTEM_ERROR),
    PARAM_ERROR(90002, "参数错误", ErrorType.CLIENT_ERROR),
    METHOD_NOT_ALLOWED(90003, "请求方法不允许", ErrorType.CLIENT_ERROR),
    REQUEST_TIMEOUT(90004, "请求超时", ErrorType.SYSTEM_ERROR),
    SERVICE_UNAVAILABLE(90005, "服务暂时不可用", ErrorType.SYSTEM_ERROR),
    DATA_INTEGRITY_VIOLATION(90006, "数据完整性冲突", ErrorType.BUSINESS_ERROR),
    OPERATION_NOT_ALLOWED(90007, "不允许的操作", ErrorType.BUSINESS_ERROR),
    DUPLICATE_SUBMISSION(90008, "请勿重复提交", ErrorType.BUSINESS_ERROR),
    RATE_LIMITED(90009, "操作过于频繁，请稍后再试", ErrorType.BUSINESS_ERROR),
    UNAUTHORIZED(90010, "未授权，请先登录", ErrorType.AUTH_ERROR),
    FORBIDDEN(90011, "权限不足", ErrorType.FORBIDDEN_ERROR),
    NOT_FOUND(90012, "资源不存在", ErrorType.NOT_FOUND_ERROR),

    // ==================== 用户模块 10000~10999 ====================
    USER_NOT_FOUND(10001, "用户不存在", ErrorType.NOT_FOUND_ERROR),
    USER_PHONE_EXISTS(10002, "手机号已注册", ErrorType.BUSINESS_ERROR),
    USER_PASSWORD_ERROR(10003, "密码错误", ErrorType.BUSINESS_ERROR),
    TOKEN_INVALID(10004, "登录已过期，请重新登录", ErrorType.AUTH_ERROR),
    USER_ACCOUNT_DISABLED(10005, "账号已被禁用或注销", ErrorType.AUTH_ERROR),
    USER_PHONE_EMPTY(10006, "手机号不能为空", ErrorType.CLIENT_ERROR),
    USER_PHONE_INVALID(10007, "手机号格式不正确", ErrorType.CLIENT_ERROR),
    USER_PASSWORD_EMPTY(10008, "密码不能为空", ErrorType.CLIENT_ERROR),
    USER_PASSWORD_WEAK(10009, "密码长度至少6位", ErrorType.CLIENT_ERROR),
    ADDRESS_LIMIT(10010, "收货地址最多20个", ErrorType.BUSINESS_ERROR),
    ADDRESS_NOT_FOUND(10011, "地址不存在", ErrorType.NOT_FOUND_ERROR),
    MEMBER_NOT_FOUND(10012, "会员不存在", ErrorType.NOT_FOUND_ERROR),

    // ==================== 商品模块 20000~20999 ====================
    PRODUCT_NOT_FOUND(20001, "商品不存在", ErrorType.NOT_FOUND_ERROR),
    PRODUCT_AUDIT_FAILED(20002, "商品审核失败", ErrorType.BUSINESS_ERROR),
    PRODUCT_ALREADY_AUDITED(20003, "该商品已审核", ErrorType.BUSINESS_ERROR),
    PRODUCT_NOT_APPROVED(20004, "仅审核通过的商品可以上下架", ErrorType.BUSINESS_ERROR),
    CATEGORY_NOT_FOUND(20005, "类目不存在", ErrorType.NOT_FOUND_ERROR),
    CATEGORY_HAS_CHILDREN(20006, "类目下有子类目，无法删除", ErrorType.BUSINESS_ERROR),
    SKU_NOT_FOUND(20007, "SKU不存在", ErrorType.NOT_FOUND_ERROR),
    STOCK_INSUFFICIENT(20008, "库存不足", ErrorType.BUSINESS_ERROR),
    STOCK_ALREADY_INIT(20009, "该SKU库存已初始化", ErrorType.BUSINESS_ERROR),
    STOCK_NOT_FOUND(20010, "库存信息不存在", ErrorType.NOT_FOUND_ERROR),

    // ==================== 订单模块 30000~30999 ====================
    ORDER_NOT_FOUND(30001, "订单不存在", ErrorType.NOT_FOUND_ERROR),
    ORDER_STATUS_ERROR(30002, "订单状态不正确", ErrorType.BUSINESS_ERROR),
    ORDER_NOT_PAID(30003, "订单未支付", ErrorType.BUSINESS_ERROR),
    PAYMENT_DUPLICATE(30004, "该订单已创建支付单", ErrorType.BUSINESS_ERROR),
    PAYMENT_NOT_FOUND(30005, "支付单不存在", ErrorType.NOT_FOUND_ERROR),
    PAYMENT_STATUS_ERROR(30006, "支付单状态不正确", ErrorType.BUSINESS_ERROR),
    REFUND_ONLY_PAID(30007, "仅已支付订单可退款", ErrorType.BUSINESS_ERROR),
    CART_ITEM_NOT_FOUND(30008, "购物车商品不存在", ErrorType.NOT_FOUND_ERROR),
    REFUND_CANCEL_FAILED(30009, "退款取消失败，订单已完成或已取消", ErrorType.BUSINESS_ERROR),
    ORDER_PAY_DUPLICATE(30010, "该订单已支付，请勿重复支付", ErrorType.BUSINESS_ERROR),

    // ==================== 营销模块 40000~40999 ====================
    COUPON_TEMPLATE_NOT_FOUND(40001, "优惠券模板不存在", ErrorType.NOT_FOUND_ERROR),
    COUPON_STOCK_INSUFFICIENT(40002, "优惠券库存不足", ErrorType.BUSINESS_ERROR),
    COUPON_CLAIM_LIMIT(40003, "已达每人领取上限", ErrorType.BUSINESS_ERROR),
    COUPON_DISABLED(40004, "该优惠券已停用", ErrorType.BUSINESS_ERROR),
    COUPON_EXPIRED(40005, "优惠券已过期", ErrorType.BUSINESS_ERROR),
    COUPON_NOT_OWNED(40006, "此优惠券不属于您", ErrorType.BUSINESS_ERROR),
    COUPON_UNAVAILABLE(40007, "优惠券不可用", ErrorType.BUSINESS_ERROR),
    SECKILL_SESSION_NOT_FOUND(40008, "秒杀场次不存在", ErrorType.NOT_FOUND_ERROR),
    SECKILL_PRODUCT_NOT_FOUND(40009, "秒杀商品不存在", ErrorType.NOT_FOUND_ERROR),
    SECKILL_STOCK_INSUFFICIENT(40010, "秒杀库存不足", ErrorType.BUSINESS_ERROR),
    SECKILL_LIMIT_REACHED(40011, "已达每人限购数量", ErrorType.BUSINESS_ERROR),
    SECKILL_NOT_ACTIVE(40012, "秒杀商品不可用", ErrorType.BUSINESS_ERROR),

    // ==================== 商家模块 50000~50999 ====================
    MERCHANT_NOT_FOUND(50001, "商家不存在", ErrorType.NOT_FOUND_ERROR),
    MERCHANT_AUDIT_FAILED(50002, "商家审核失败", ErrorType.BUSINESS_ERROR),
    MERCHANT_ALREADY_APPLIED(50003, "该用户已提交入驻申请", ErrorType.BUSINESS_ERROR),
    MERCHANT_ALREADY_AUDITED(50004, "该商家已审核", ErrorType.BUSINESS_ERROR),
    SHOP_NOT_FOUND(50005, "店铺不存在", ErrorType.NOT_FOUND_ERROR),

    // ==================== 平台模块 60000~60999 ====================
    BANNER_NOT_FOUND(60001, "Banner不存在", ErrorType.NOT_FOUND_ERROR),
    RISK_RULE_NOT_FOUND(60002, "风控规则不存在", ErrorType.NOT_FOUND_ERROR),

    // ==================== 文件模块 70000~70999 ====================
    FILE_UPLOAD_FAILED(70001, "文件上传失败", ErrorType.SYSTEM_ERROR),
    FILE_NOT_FOUND(70002, "文件不存在", ErrorType.NOT_FOUND_ERROR),
    FILE_TYPE_NOT_ALLOWED(70003, "文件类型不允许", ErrorType.CLIENT_ERROR),
    FILE_SIZE_EXCEEDED(70004, "文件大小超出限制", ErrorType.CLIENT_ERROR),
    FILE_EMPTY(70005, "文件不能为空", ErrorType.CLIENT_ERROR),
    FILE_READ_FAILED(70006, "读取文件失败", ErrorType.SYSTEM_ERROR),
    FILE_DOWNLOAD_FAILED(70007, "文件下载失败", ErrorType.SYSTEM_ERROR),
    NOTIFY_TEMPLATE_NOT_FOUND(70008, "通知模板不存在", ErrorType.NOT_FOUND_ERROR),
    NOTIFY_NOT_FOUND(70009, "通知不存在", ErrorType.NOT_FOUND_ERROR),
    NOTIFY_SEND_FAILED(70010, "通知发送失败", ErrorType.SYSTEM_ERROR),

    // ==================== 评价模块 80000~80999 ====================
    REVIEW_NOT_FOUND(80001, "评价不存在", ErrorType.NOT_FOUND_ERROR),
    REVIEW_RATING_INVALID(80002, "评分必须在1-5之间", ErrorType.CLIENT_ERROR),
    REVIEW_ORDER_REQUIRED(80003, "订单号不能为空", ErrorType.CLIENT_ERROR),
    REVIEW_PRODUCT_REQUIRED(80004, "商品ID不能为空", ErrorType.CLIENT_ERROR),
    REVIEW_NOT_OWNED(80005, "只能操作自己的评价", ErrorType.BUSINESS_ERROR);

    private final int code;
    private final String message;
    private final ErrorType errorType;
}
