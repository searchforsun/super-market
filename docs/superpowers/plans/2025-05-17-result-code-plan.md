# 优化后的完整ResultCode架构实现

我为你生成了**生产级可用**的完整错误码体系，包含：
- 修复了区间冲突问题
- 扩展了模块预留空间到1000个
- 添加了错误类型分类
- 补充了缺失的通用错误码
- 合并了语义重复的错误码
- 提供了完整的全局异常处理器
- 支持动态消息和国际化扩展

## 一、核心枚举类

### 1.1 IResultCode 接口
```java
package com.supermarket.common.core.result;

/**
 * 错误码接口
 */
public interface IResultCode {

    /**
     * 获取错误码
     */
    int getCode();

    /**
     * 获取错误信息
     */
    String getMessage();

    /**
     * 获取错误类型
     */
    ErrorType getErrorType();
}
```

### 1.2 ErrorType 错误类型枚举
```java
package com.supermarket.common.core.result;

/**
 * 错误类型枚举
 * 用于全局异常处理器统一映射HTTP状态码和日志级别
 */
public enum ErrorType {

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 客户端错误（参数错误、请求格式错误等）
     */
    CLIENT_ERROR,

    /**
     * 认证错误（未登录、Token无效等）
     */
    AUTH_ERROR,

    /**
     * 授权错误（权限不足等）
     */
    FORBIDDEN_ERROR,

    /**
     * 资源不存在错误
     */
    NOT_FOUND_ERROR,

    /**
     * 业务规则错误（库存不足、订单状态错误等）
     */
    BUSINESS_ERROR,

    /**
     * 系统内部错误
     */
    SYSTEM_ERROR
}
```

### 1.3 ResultCode 错误码枚举（优化版）
```java
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
```

## 二、统一响应体

```java
package com.supermarket.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    // ==================== 成功响应 ====================

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ==================== 失败响应 ====================

    public static <T> Result<T> fail(IResultCode resultCode) {
        return fail(resultCode, resultCode.getMessage());
    }

    public static <T> Result<T> fail(IResultCode resultCode, String message) {
        return Result.<T>builder()
                .code(resultCode.getCode())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ==================== 便捷方法 ====================

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    public boolean isFail() {
        return !isSuccess();
    }
}
```

## 三、业务异常类

```java
package com.supermarket.common.core.exception;

import com.supermarket.common.core.result.IResultCode;
import lombok.Getter;

/**
 * 业务异常
 * 所有业务逻辑抛出的异常都应该使用此类
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final IResultCode resultCode;

    public BusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(IResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(IResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }

    public BusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }
}
```

## 四、全局异常处理器（核心）

```java
package com.supermarket.common.core.exception;

import com.supermarket.common.core.result.ErrorType;
import com.supermarket.common.core.result.IResultCode;
import com.supermarket.common.core.result.Result;
import com.supermarket.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 自动根据错误类型映射到正确的HTTP状态码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        IResultCode resultCode = e.getResultCode();
        HttpStatus status = mapToHttpStatus(resultCode.getErrorType());
        
        // 系统错误打印完整堆栈，业务错误只打印简要信息
        if (resultCode.getErrorType() == ErrorType.SYSTEM_ERROR) {
            log.error("业务系统异常: code={}, message={}", resultCode.getCode(), e.getMessage(), e);
        } else {
            log.warn("业务异常: code={}, message={}", resultCode.getCode(), e.getMessage());
        }
        
        return new ResponseEntity<>(Result.fail(resultCode, e.getMessage()), status);
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验异常: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.PARAM_ERROR, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数绑定异常（@RequestParam）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定异常: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.PARAM_ERROR, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数校验异常（@Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验异常: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.PARAM_ERROR, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数'%s'类型不匹配，期望类型'%s'", e.getName(), e.getRequiredType().getSimpleName());
        
        log.warn("参数类型异常: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.PARAM_ERROR, message), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("请求方法'%s'不支持，支持的方法有'%s'", 
                e.getMethod(), String.join(", ", e.getSupportedMethods()));
        
        log.warn("请求方法异常: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.METHOD_NOT_ALLOWED, message), HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("请求路径'%s'不存在", e.getRequestURL());
        
        log.warn("资源不存在: {}", message);
        return new ResponseEntity<>(Result.fail(ResultCode.NOT_FOUND, message), HttpStatus.NOT_FOUND);
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统未捕获异常", e);
        return new ResponseEntity<>(Result.fail(ResultCode.SYSTEM_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 将错误类型映射到HTTP状态码
     */
    private HttpStatus mapToHttpStatus(ErrorType errorType) {
        switch (errorType) {
            case SUCCESS:
                return HttpStatus.OK;
            case CLIENT_ERROR:
                return HttpStatus.BAD_REQUEST;
            case AUTH_ERROR:
                return HttpStatus.UNAUTHORIZED;
            case FORBIDDEN_ERROR:
                return HttpStatus.FORBIDDEN;
            case NOT_FOUND_ERROR:
                return HttpStatus.NOT_FOUND;
            case BUSINESS_ERROR:
                return HttpStatus.CONFLICT; // 409 Conflict 表示业务规则冲突
            case SYSTEM_ERROR:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
```

## 五、使用示例

### 5.1 业务层抛出异常
```java
@Service
public class OrderService {

    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待付款订单可取消");
        }
        
        // 取消订单逻辑
    }
}
```

### 5.2 控制器返回结果
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}")
    public Result<OrderVO> getOrderById(@PathVariable Long id) {
        OrderVO order = orderService.getOrderById(id);
        return Result.success(order);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }
}
```

## 六、关键优化点说明

1. **错误类型驱动的HTTP状态码映射**：不再需要手动维护code到HTTP状态码的映射表，通过ErrorType自动映射，扩展性更好

2. **精细化的日志级别**：系统错误打印完整堆栈，业务错误只打印简要信息，避免日志泛滥

3. **统一的参数校验处理**：覆盖了所有常见的参数校验异常场景，自动拼接错误信息

4. **扩展预留充足**：每个模块预留了1000个错误码，足够支撑大型项目的发展

5. **向后兼容**：前端仍然可以通过响应体中的code字段判断业务结果，不需要修改现有逻辑

6. **国际化支持**：如果后续需要国际化，只需要将message改为消息key，在全局异常处理器中进行翻译即可


# 前端拦截示例
```javascript
// src/utils/request-enhanced.js
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 重复请求取消器
const pendingRequests = new Map()

// 创建Axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 错误重试配置
  retry: 2, // 重试次数
  retryDelay: 1000 // 重试间隔(ms)
})

// 生成请求唯一标识
const generateRequestKey = (config) => {
  const { method, url, params, data } = config
  return [method, url, JSON.stringify(params), JSON.stringify(data)].join('&')
}

// 添加待处理请求
const addPendingRequest = (config) => {
  const requestKey = generateRequestKey(config)
  config.cancelToken = config.cancelToken || new axios.CancelToken((cancel) => {
    if (!pendingRequests.has(requestKey)) {
      pendingRequests.set(requestKey, cancel)
    }
  })
}

// 移除待处理请求
const removePendingRequest = (config) => {
  const requestKey = generateRequestKey(config)
  if (pendingRequests.has(requestKey)) {
    const cancel = pendingRequests.get(requestKey)
    cancel('重复请求已取消')
    pendingRequests.delete(requestKey)
  }
}

// 取消所有待处理请求
export const cancelAllPendingRequests = () => {
  pendingRequests.forEach((cancel) => {
    cancel('页面跳转，请求已取消')
  })
  pendingRequests.clear()
}

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    
    // 取消重复请求
    removePendingRequest(config)
    addPendingRequest(config)
    
    // 添加Token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    
    // 添加请求时间戳，防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }
    
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    removePendingRequest(response.config)
    const res = response.data
    
    // 业务成功
    if (res.code === 0) {
      return res
    }
    
    // 业务失败
    ElMessage.error({
      message: res.message || '请求失败',
      duration: 5 * 1000
    })
    
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async (error) => {
    console.error('响应错误:', error)
    
    const config = error.config
    removePendingRequest(config)
    
    // 如果是取消请求，不显示错误
    if (axios.isCancel(error)) {
      console.log('请求已取消:', error.message)
      return Promise.reject(error)
    }
    
    // 错误重试逻辑
    if (config && config.retry && !config.__retryCount) {
      config.__retryCount = 0
    }
    
    if (config && config.__retryCount < config.retry) {
      config.__retryCount += 1
      console.log(`请求重试 ${config.__retryCount}/${config.retry}`)
      
      // 延迟重试
      await new Promise(resolve => setTimeout(resolve, config.retryDelay))
      return service(config)
    }
    
    const userStore = useUserStore()
    let message = '系统繁忙，请稍后重试'
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          message = data.message || '参数错误'
          break
        case 401:
          // 防止重复弹出登录框
          if (!window.__isLoginDialogOpen) {
            window.__isLoginDialogOpen = true
            
            ElMessageBox.confirm(
              '登录已过期，请重新登录',
              '系统提示',
              {
                confirmButtonText: '重新登录',
                cancelButtonText: '取消',
                type: 'warning',
                closeOnClickModal: false
              }
            ).then(() => {
              userStore.logout()
              router.push('/login')
            }).finally(() => {
              window.__isLoginDialogOpen = false
            })
          }
          return Promise.reject(error)
        case 403:
          message = '权限不足，无法访问'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 409:
          // 业务规则冲突，直接显示后端返回的消息
          message = data.message || '操作失败'
          break
        case 429:
          message = '操作过于频繁，请稍后再试'
          break
        case 500:
          message = '系统内部错误'
          break
        case 503:
          message = '服务暂时不可用'
          break
        default:
          message = `请求失败，状态码：${status}`
      }
    } else if (error.message.includes('timeout')) {
      message = '请求超时，请检查网络连接'
    } else if (error.message.includes('Network Error')) {
      message = '网络连接失败，请检查网络'
    }
    
    ElMessage.error({
      message,
      duration: 5 * 1000
    })
    
    return Promise.reject(error)
  }
)

// 封装常用请求方法
export const request = {
  get(url, params, options = {}) {
    return service.get(url, { params, ...options })
  },
  
  post(url, data, options = {}) {
    return service.post(url, data, options)
  },
  
  put(url, data, options = {}) {
    return service.put(url, data, options)
  },
  
  delete(url, params, options = {}) {
    return service.delete(url, { params, ...options })
  },
  
  // 文件上传
  upload(url, file, onProgress, options = {}) {
    const formData = new FormData()
    formData.append('file', file)
    
    return service.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      },
      ...options
    })
  },
  
  // 文件下载
  download(url, params, filename, options = {}) {
    return service.get(url, {
      params,
      responseType: 'blob',
      ...options
    }).then((res) => {
      const blob = new Blob([res.data])
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = filename || 'download'
      link.click()
      window.URL.revokeObjectURL(downloadUrl)
    })
  }
}

// 路由跳转时取消所有请求
router.beforeEach((to, from, next) => {
  cancelAllPendingRequests()
  next()
})

export default service

```