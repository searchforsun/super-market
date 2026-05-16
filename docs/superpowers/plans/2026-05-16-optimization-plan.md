# 超级市场电商平台 — 功能优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目从功能原型（6.5/10）提升至接近生产就绪（8.5/10），覆盖安全基线、功能堵漏、前端补全、可观测性、K8s 和 CI/CD。

**Architecture:** 按 P0→P1→P2 三阶段推进。P0 建立安全底线（RBAC、输入校验、密钥脱敏），P1 补齐服务端缺失接口、统一错误码、补全 B2C 前端 9 个页面，P2 接入可观测性、补齐 K8s 清单、优化 CI/CD。

**Tech Stack:** Java 21, Spring Boot 3.2.5, Dubbo 3.2.13, Spring Cloud Gateway 4.1, Vue 3 + TypeScript + Pinia + Element Plus, Docker Compose, K8s 1.29, GitLab CI

---

## 文件结构总览

```
新增文件:
  super-market-gateway/src/main/java/.../filter/RoleBasedFilter.java
  super-market-common/common-web/src/main/java/.../annotation/RequireRole.java
  super-market-common/common-web/src/main/java/.../interceptor/RoleInterceptor.java
  super-market-common/common-web/src/main/java/.../config/WebMvcConfig.java
  super-market-common/common-core/src/main/java/.../IResultCode.java
  super-market-common/common-core/src/main/java/.../ResultCode.java
  super-market-services/service-seckill/.../mq/SeckillOrderConsumer.java
  super-market-k8s/base/secret.yaml.example
  super-market-k8s/apps/service-*.yaml (19 个 Service)
  super-market-k8s/base/ingress.yaml
  super-market-k8s/apps/hpa-*.yaml (5 个 HPA)
  super-market-k8s/statefulset/mysql.yaml
  super-market-k8s/statefulset/redis.yaml
  super-market-k8s/base/network-policy.yaml
  docker-compose/grafana/provisioning/dashboards/jvm-overview.json
  docker-compose/grafana/provisioning/dashboards/dubbo-rpc.json
  docker-compose/grafana/provisioning/dashboards/business-qps.json
  docker-compose/grafana/provisioning/dashboards/error-top10.json
  frontend/packages/stores/src/useAddressStore.ts
  frontend/packages/stores/src/useOrderStore.ts
  frontend/packages/stores/src/useCouponStore.ts
  frontend/packages/stores/src/useNotifyStore.ts

修改文件:
  18 个 application-dev.yml (脱敏)
  JwtProperties.java (删除默认 secret)
  BizException.java (新增枚举构造器)
  GlobalExceptionHandler.java (补齐 ConstraintViolationException)
  PaymentServiceImpl.java (加 implements + @DubboService)
  OrderServiceImpl.java (移除 shopId=1L)
  CreateOrderRequest.java (加 shopId 字段)
  OrderController.java (新增 listByShop 端点)
  MemberController.java (新增 deductPoints 端点)
  FileController.java (新增 download 端点)
  NotifyController.java (新增 send 端点)
  ProductController.java + ProductService.java (新增 update 端点)
  AuthController.java + UserController.java + PaymentController.java (加校验注解)
  18 个服务 pom.xml (加 actuator + micrometer)
  docker-compose/prometheus/prometheus.yml (取消注释 scrape)
  .gitignore (追加 secret.yaml)
  .gitlab-ci.yml (parallel: matrix + security stage)
  20 个 Dockerfile (优雅关闭 JVM 参数)
  B2C 9 个页面 (重写/补全)
  B2C 路由 index.ts (无需改动，路由已存在)

删除文件:
  super-market-k8s/base/secret.yaml
```

---

## 第 1 周 · P0 安全底线

### Task 1: RoleBasedFilter（Gateway 层 RBAC）

**Files:**
- Create: `super-market-gateway/src/main/java/com/supermarket/gateway/filter/RoleBasedFilter.java`

- [ ] **Step 1: 新建 RoleBasedFilter.java**

```java
package com.supermarket.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RoleBasedFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITELIST = List.of(
        "/api/user/register",
        "/api/user/login",
        "/api/auth/login",
        "/api/search",
        "/actuator",
        "/doc.html",
        "/swagger-ui",
        "/webjars",
        "/v3/api-docs",
        "/favicon.ico"
    );

    private static final Map<String, Set<String>> PATH_ROLE_MAP = Map.of(
        "/api/platform/admin", Set.of("ROLE_ADMIN"),
        "/api/coupon/admin",   Set.of("ROLE_ADMIN", "ROLE_MERCHANT"),
        "/api/seckill/admin",  Set.of("ROLE_ADMIN", "ROLE_MERCHANT"),
        "/api/shop/merchant",  Set.of("ROLE_MERCHANT"),
        "/api/order/admin",    Set.of("ROLE_ADMIN")
    );

    private static final Set<String> ALL_ROLES = Set.of("ROLE_USER", "ROLE_MERCHANT", "ROLE_ADMIN");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单不过滤
        if (WHITELIST.stream().anyMatch(path::startsWith)
            || path.contains("v3/api-docs")
            || path.contains("swagger-ui")
            || path.contains("webjars")) {
            return chain.filter(exchange);
        }

        String rolesHeader = request.getHeaders().getFirst("X-User-Roles");
        Set<String> userRoles = (rolesHeader != null && !rolesHeader.isEmpty())
            ? Set.of(rolesHeader.split(","))
            : Set.of();

        // 匹配路径→角色
        for (var entry : PATH_ROLE_MAP.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                boolean hasRole = userRoles.stream().anyMatch(entry.getValue()::contains);
                if (!hasRole) {
                    return forbidden(exchange, "权限不足: 需要 " + entry.getValue());
                }
                return chain.filter(exchange);
            }
        }

        // 其余路径仅需登录（至少有一个角色）
        if (userRoles.isEmpty() || userRoles.stream().noneMatch(ALL_ROLES::contains)) {
            return forbidden(exchange, "未授权访问");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":403,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl super-market-gateway compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add super-market-gateway/src/main/java/com/supermarket/gateway/filter/RoleBasedFilter.java
git commit -m "feat: add RoleBasedFilter for gateway-level RBAC
```

---

### Task 2: RequireRole 注解 + RoleInterceptor（服务端层 RBAC）

**Files:**
- Create: `super-market-common/common-web/src/main/java/com/supermarket/common/web/annotation/RequireRole.java`
- Create: `super-market-common/common-web/src/main/java/com/supermarket/common/web/interceptor/RoleInterceptor.java`
- Create: `super-market-common/common-web/src/main/java/com/supermarket/common/web/config/WebMvcConfig.java`

- [ ] **Step 1: 新建 RequireRole 注解**

```java
package com.supermarket.common.web.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value() default {};
}
```

- [ ] **Step 2: 新建 RoleInterceptor**

```java
package com.supermarket.common.web.interceptor;

import com.supermarket.common.core.constants.GlobalConstants;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.web.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequireRole annotation = hm.getMethodAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (annotation == null) return true; // 无注解，Gateway 兜底

        String rolesHeader = request.getHeader(GlobalConstants.USER_ROLES);
        Set<String> userRoles = (rolesHeader != null && !rolesHeader.isEmpty())
            ? Set.of(rolesHeader.split(","))
            : Set.of();

        boolean hasRole = Arrays.stream(annotation.value()).anyMatch(userRoles::contains);
        if (!hasRole) {
            throw new BizException(403, "权限不足");
        }
        return true;
    }
}
```

- [ ] **Step 3: 新建 WebMvcConfig 注册拦截器**

```java
package com.supermarket.common.web.config;

import com.supermarket.common.web.interceptor.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor).order(1);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-common/common-web compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-web/src/main/java/com/supermarket/common/web/annotation/RequireRole.java \
        super-market-common/common-web/src/main/java/com/supermarket/common/web/interceptor/RoleInterceptor.java \
        super-market-common/common-web/src/main/java/com/supermarket/common/web/config/WebMvcConfig.java
git commit -m "feat: add RequireRole annotation and RoleInterceptor for server-side RBAC"
```

---

### Task 3: PaymentDubboService 修复

**Files:**
- Modify: `super-market-services/service-payment/src/main/java/com/supermarket/payment/service/impl/PaymentServiceImpl.java:26-27`

- [ ] **Step 1: 补 implements 声明和 @DubboService**

```java
// 修改第 26 行：加 @DubboService 注解
@DubboService(interfaceClass = PaymentDubboService.class)
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService, PaymentDubboService {
```

同时需要新增 import：
```java
import com.supermarket.common.dubbo.api.payment.PaymentDubboService;
```

Note: `PaymentDubboService` 的 `createPayment` 返回 `PaymentDTO`，而 `PaymentServiceImpl.createPayment` 返回 `Payment`。需要在类中新增一个适配方法或调整返回类型。当前设计文档中 Dubbo 接口签名与本地 Service 签名不同——`PaymentDubboService.createPayment` 返回 `PaymentDTO`，`PaymentService.createPayment` 返回 `Payment`。

需新增 `PaymentDubboService` 的 3 个方法适配：

```java
// 新增 PaymentDubboService 方法适配（在类末尾添加）

@Override
public PaymentDTO createPayment(String orderNo, Long userId, java.math.BigDecimal amount, Integer payMethod) {
    Payment p = createPayment(orderNo, userId, amount, payMethod); // 调用本地实现
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

@Override
public PaymentDTO getByPayNo(String payNo) {
    Payment p = getByPayNo(payNo);
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

@Override
public void handleCallback(String payNo, String thirdPayNo) {
    String requestId = "DUBBO_" + payNo + "_" + System.currentTimeMillis();
    handleCallback(requestId, payNo, thirdPayNo);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl super-market-services/service-payment compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add super-market-services/service-payment/src/main/java/com/supermarket/payment/service/impl/PaymentServiceImpl.java
git commit -m "fix: implement PaymentDubboService in PaymentServiceImpl"
```

---

### Task 4: 密钥脱敏 — application-dev.yml

**Files:**
- Modify: 18 个 `super-market-services/service-*/src/main/resources/application-dev.yml`
- Modify: `super-market-gateway/src/main/resources/application-dev.yml` (if exists)

- [ ] **Step 1: 逐文件改密码行（以 service-user 为例）**

```yaml
# 改前 (line 9)
    password: ${MYSQL_ROOT_PASSWORD:root123}
# 改后
    password: ${MYSQL_ROOT_PASSWORD:}

# 改前 (line 22)
      password: ${REDIS_PASSWORD:redis123}
# 改后
      password: ${REDIS_PASSWORD:}
```

对以下全部 19 个文件执行同样替换：
```
super-market-gateway/src/main/resources/application-dev.yml
super-market-services/service-user/src/main/resources/application-dev.yml
super-market-services/service-auth/src/main/resources/application-dev.yml
super-market-services/service-member/src/main/resources/application-dev.yml
super-market-services/service-address/src/main/resources/application-dev.yml
super-market-services/service-product/src/main/resources/application-dev.yml
super-market-services/service-category/src/main/resources/application-dev.yml
super-market-services/service-inventory/src/main/resources/application-dev.yml
super-market-services/service-review/src/main/resources/application-dev.yml
super-market-services/service-cart/src/main/resources/application-dev.yml
super-market-services/service-order/src/main/resources/application-dev.yml
super-market-services/service-payment/src/main/resources/application-dev.yml
super-market-services/service-coupon/src/main/resources/application-dev.yml
super-market-services/service-seckill/src/main/resources/application-dev.yml
super-market-services/service-search/src/main/resources/application-dev.yml
super-market-services/service-shop/src/main/resources/application-dev.yml
super-market-services/service-platform/src/main/resources/application-dev.yml
super-market-services/service-file/src/main/resources/application-dev.yml
super-market-services/service-notify/src/main/resources/application-dev.yml
```

- [ ] **Step 2: 验证 .env 文件提供变量**

确认 `docker-compose/.env` 中定义了 `MYSQL_ROOT_PASSWORD` 和 `REDIS_PASSWORD`（开发者已有该文件）。

- [ ] **Step 3: 编译验证（随机抽 3 个服务）**

```bash
mvn -pl super-market-services/service-user,super-market-services/service-order,super-market-gateway compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-*/src/main/resources/application-dev.yml super-market-gateway/src/main/resources/application-dev.yml
git commit -m "security: replace hardcoded passwords with env vars in all application-dev.yml files"
```

---

### Task 5: 密钥脱敏 — JwtProperties + K8s secret + .gitignore

**Files:**
- Modify: `super-market-common/common-security/src/main/java/com/supermarket/common/security/config/JwtProperties.java`
- Delete: `super-market-k8s/base/secret.yaml`
- Create: `super-market-k8s/base/secret.yaml.example`
- Modify: `.gitignore`

- [ ] **Step 1: 删除 JwtProperties 默认 secret**

```java
// JwtProperties.java line 12 改前:
    private String secret = "super-market-default-secret-key-change-in-production-min-256-bits";

// 改后:
    private String secret;
```

启动时的校验可加在 JwtUtil 的 `@PostConstruct` 中（如 JwtUtil 无此逻辑，则在 parseToken 时以 null-safe 方式处理——jjwt 在 key 为空时会抛出明确异常）。

- [ ] **Step 2: 删除 secret.yaml，新建 secret.yaml.example**

```bash
git rm super-market-k8s/base/secret.yaml
```

新建 `super-market-k8s/base/secret.yaml.example`（内容与原 secret.yaml 相同，但所有值改为 `change_me`）：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: super-market-secrets
  namespace: super-market
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: "change_me"
  MYSQL_APP_PASSWORD: "change_me"
  REDIS_PASSWORD: "change_me"
  JWT_SECRET: "change_me"
  MINIO_ROOT_PASSWORD: "change_me"
  MINIO_ROOT_USER: "admin"
```

- [ ] **Step 3: .gitignore 追加**

```
# === Kubernetes secrets ===
**/secret.yaml
!**/secret.yaml.example
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-common/common-security compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-security/src/main/java/com/supermarket/common/security/config/JwtProperties.java \
        super-market-k8s/base/secret.yaml.example \
        .gitignore
git commit -m "security: remove default JWT secret, delete K8s secret.yaml from repo"
```

---

### Task 6: 输入校验 — AuthController + UserController

**Files:**
- Modify: `super-market-services/service-auth/src/main/java/com/supermarket/auth/controller/AuthController.java`
- Modify: `super-market-services/service-user/src/main/java/com/supermarket/user/controller/UserController.java`
- Modify: `super-market-common/common-web/src/main/java/com/supermarket/common/web/handler/GlobalExceptionHandler.java`

- [ ] **Step 1: AuthController 加校验**

```java
// AuthController.java — login 方法
@PostMapping("/login")
@Operation(summary = "用户登录")
public R<Map<String, Object>> login(
    @RequestParam @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
    @RequestParam @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位") String password) {
    return R.ok(authService.login(phone, password));
}

// refresh 方法
@PostMapping("/refresh")
@Operation(summary = "刷新Token")
public R<Map<String, Object>> refresh(
    @RequestParam @NotBlank(message = "refreshToken不能为空") String refreshToken) {
    return R.ok(authService.refresh(refreshToken));
}
```

需要新增 import：
```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
```

- [ ] **Step 2: UserController 加校验**

```java
// UserController.java — register 方法
@PostMapping("/register")
@Operation(summary = "用户注册")
public R<User> register(
    @RequestParam @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
    @RequestParam @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位") String password) {
    return R.ok(userService.register(phone, password));
}

// login 方法
@PostMapping("/login")
@Operation(summary = "用户密码登录")
public R<User> login(
    @RequestParam @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
    @RequestParam @NotBlank(message = "密码不能为空") String password) {
    return R.ok(userService.login(phone, password));
}
```

- [ ] **Step 3: GlobalExceptionHandler 补齐 ConstraintViolationException**

```java
// GlobalExceptionHandler.java — 新增处理方法

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ConstraintViolation;
import java.util.stream.Collectors;

@ExceptionHandler(ConstraintViolationException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public R<Void> handleConstraintViolation(ConstraintViolationException e) {
    String message = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    return R.fail(400, message);
}
```

需要在类上加 `@Validated` 或在 Controller 类上加 `@Validated` 注解使 `@RequestParam` 校验生效。在各 Controller 类上加：
```java
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
public class AuthController { ... }
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-services/service-auth,super-market-services/service-user,super-market-common/common-web compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-auth/src/main/java/com/supermarket/auth/controller/AuthController.java \
        super-market-services/service-user/src/main/java/com/supermarket/user/controller/UserController.java \
        super-market-common/common-web/src/main/java/com/supermarket/common/web/handler/GlobalExceptionHandler.java
git commit -m "feat: add input validation to AuthController and UserController"
```

---

### Task 7: 输入校验 — PaymentController + OrderController

**Files:**
- Modify: `super-market-services/service-payment/src/main/java/com/supermarket/payment/controller/PaymentController.java`
- Modify: `super-market-services/service-order/src/main/java/com/supermarket/order/controller/OrderController.java`

- [ ] **Step 1: PaymentController 加校验和 @Validated**

```java
// 类上加 @Validated
@Validated
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @PostMapping("/pay")
    public R<Payment> pay(
        @RequestParam @NotBlank(message = "订单号不能为空") String orderNo,
        @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
        @RequestParam @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于0") BigDecimal amount,
        @RequestParam(defaultValue = "1") Integer payMethod) { ... }

    @PostMapping("/refund")
    public R<PaymentRefund> refund(
        @RequestParam @NotBlank(message = "订单号不能为空") String orderNo,
        @RequestParam @NotNull(message = "退款金额不能为空")
        @DecimalMin(value = "0.01", message = "退款金额必须大于0") BigDecimal refundAmount,
        @RequestParam @Size(max = 500, message = "退款原因最长500字") String reason) { ... }
}
```

- [ ] **Step 2: OrderController 加校验和 @Validated**

```java
@Validated
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @PostMapping("/create")
    public R<Order> create(@Valid @RequestBody CreateOrderRequest request) { ... }

    @PutMapping("/{orderNo}/cancel")
    public R<Void> cancel(
        @PathVariable @NotBlank(message = "订单号不能为空") String orderNo,
        @RequestParam @Size(max = 500, message = "取消原因最长500字") String reason) { ... }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn -pl super-market-services/service-payment,super-market-services/service-order compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-payment/src/main/java/com/supermarket/payment/controller/PaymentController.java \
        super-market-services/service-order/src/main/java/com/supermarket/order/controller/OrderController.java
git commit -m "feat: add input validation to PaymentController and OrderController"
```

---

### Task 8: 输入校验 — Product/Coupon/Shop 审核接口（第 3 批）

**Files:**
- Modify: `super-market-services/service-product/src/main/java/com/supermarket/product/controller/ProductController.java`
- Modify: `super-market-services/service-coupon/src/main/java/com/supermarket/coupon/controller/CouponController.java`
- Modify: `super-market-services/service-shop/src/main/java/com/supermarket/shop/controller/ShopController.java`

- [ ] **Step 1: ProductController 加 @Validated**

已在 product 的 audit 端点方法上加校验——当前 audit 方法接受 `@RequestParam Integer auditStatus, String reason`。添加：
```java
@Validated  // 类级别
public class ProductController {

    @PutMapping("/spu/{spuId}/audit")
    public R<Void> audit(
        @PathVariable Long spuId,
        @RequestParam @NotNull(message = "审核状态不能为空")
        @Min(value = 1, message = "审核状态值无效") @Max(value = 3, message = "审核状态值无效")
        Integer auditStatus,
        @RequestParam(required = false) @Size(max = 500) String reason) { ... }
}
```

- [ ] **Step 2: CouponController + ShopController 加 @Validated**

```java
// CouponController — admin 端点
@Validated
public class CouponController { ... }

// ShopController — audit 端点
@Validated
public class ShopController {
    @PutMapping("/merchant/{id}/audit")
    public R<Void> audit(
        @PathVariable Long id,
        @RequestParam @NotNull Integer auditStatus,
        @RequestParam(required = false) @Size(max = 500) String reason) { ... }
}
```

- [ ] **Step 3: 其余 Controller 加 @Validated（第 4 批）**

对其余 12 个 Controller 类级别加 `@Validated`（不改方法签名，仅让框架支持 `@RequestParam` 约束）：
AddressController, CartController, CategoryController, FileController, InventoryController, MemberController, NotifyController, PlatformController, ReviewController, SearchController, SeckillController（均在类上加 `@Validated`）。

- [ ] **Step 4: 编译验证**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS（全模块编译通过）

- [ ] **Step 5: Commit**

```bash
git add super-market-services/
git commit -m "feat: add @Validated and validation annotations to all controllers"
```

---

## 第 2 周 · P1 服务端堵漏

### Task 9: service-order — listByShop 端点 + 移除 shopId 硬编码

**Files:**
- Modify: `super-market-services/service-order/src/main/java/com/supermarket/order/controller/OrderController.java`
- Modify: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/order/dto/CreateOrderRequest.java`
- Modify: `super-market-services/service-order/src/main/java/com/supermarket/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: CreateOrderRequest 加 shopId 字段**

```java
// CreateOrderRequest.java — 在 userId 之后新增字段
private Long shopId;
```

- [ ] **Step 2: OrderController 新增端点**

```java
// OrderController.java — 新增方法
@GetMapping("/list/shop/{shopId}")
@Operation(summary = "查询店铺订单列表")
public R<Page<Order>> listByShop(
    @Parameter(description = "店铺ID") @PathVariable Long shopId,
    @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
    @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
    @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
    return R.ok(orderService.listByShop(shopId, status, page, size));
}
```

- [ ] **Step 3: OrderServiceImpl 移除 shopId=1L**

```java
// OrderServiceImpl.java line 59 改前:
    order.setShopId(1L);
// 改后:
    order.setShopId(request.getShopId() != null ? request.getShopId() : 1L);
```

保留 1L 作为兜底（防止老调用方未传 shopId），但不再硬编码。

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-services/service-order,super-market-common/common-dubbo-api compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-order/src/main/java/com/supermarket/order/controller/OrderController.java \
        super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/order/dto/CreateOrderRequest.java \
        super-market-services/service-order/src/main/java/com/supermarket/order/service/impl/OrderServiceImpl.java
git commit -m "feat: add listByShop endpoint, fix hardcoded shopId in createOrder"
```

---

### Task 10: service-seckill — SeckillOrderConsumer

**Files:**
- Create: `super-market-services/service-seckill/src/main/java/com/supermarket/seckill/mq/SeckillOrderConsumer.java`

- [ ] **Step 1: 新建 SeckillOrderConsumer**

```java
package com.supermarket.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RocketMQTemplate.class)
@RocketMQMessageListener(
    topic = "seckill-order-topic",
    consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements RocketMQListener<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DubboReference(check = false)
    private OrderDubboService orderDubboService;

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(String messageJson) {
        log.info("收到秒杀订单消息: {}", messageJson);
        try {
            Map<String, Object> msg = MAPPER.readValue(messageJson, Map.class);
            Long userId = Long.valueOf(msg.get("userId").toString());
            Long skuId = Long.valueOf(msg.get("skuId").toString());
            BigDecimal price = new BigDecimal(msg.get("price").toString());
            int quantity = Integer.parseInt(msg.get("quantity").toString());

            CreateOrderRequest request = new CreateOrderRequest();
            request.setUserId(userId);
            request.setItems(List.of(buildItem(skuId, price, quantity)));

            orderDubboService.createOrder(request);
            log.info("秒杀订单创建成功: userId={}, skuId={}", userId, skuId);
        } catch (Exception e) {
            log.error("秒杀订单创建失败: message={}", messageJson, e);
        }
    }

    private CreateOrderRequest.OrderItemRequest buildItem(Long skuId, BigDecimal price, int quantity) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(skuId);
        item.setSkuPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl super-market-services/service-seckill compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add super-market-services/service-seckill/src/main/java/com/supermarket/seckill/mq/SeckillOrderConsumer.java
git commit -m "feat: add SeckillOrderConsumer for async order creation after flash sale"
```

---

### Task 11: B 组缺失接口 — MemberController + FileController + NotifyController

**Files:**
- Modify: `super-market-services/service-member/src/main/java/com/supermarket/member/controller/MemberController.java`
- Modify: `super-market-services/service-file/src/main/java/com/supermarket/file/controller/FileController.java`
- Modify: `super-market-services/service-notify/src/main/java/com/supermarket/notify/controller/NotifyController.java`

- [ ] **Step 1: MemberController 新增 deductPoints**

```java
@PostMapping("/points/deduct")
@Operation(summary = "扣减积分")
public R<Void> deductPoints(
    @RequestParam @NotNull Long userId,
    @RequestParam @NotNull @Min(1) Integer points,
    @RequestParam @NotBlank String reason) {
    memberService.deductPoints(userId, points, reason);
    return R.ok();
}
```

- [ ] **Step 2: FileController 新增 download**

```java
@GetMapping("/{id}/download")
@Operation(summary = "下载文件")
public ResponseEntity<byte[]> download(@PathVariable Long id) {
    var record = fileService.getById(id);
    byte[] data = fileService.download(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8) + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(data);
}
```

需要新增 import：`org.springframework.http.ResponseEntity`, `HttpHeaders`, `MediaType`, `URLEncoder`, `java.nio.charset.StandardCharsets`

- [ ] **Step 3: NotifyController 新增 send**

```java
@PostMapping("/send")
@Operation(summary = "发送通知（内部服务调用）")
public R<Void> send(
    @RequestHeader(value = "X-Source-Service", required = false) String sourceService,
    @RequestParam @NotBlank String templateCode,
    @RequestParam @NotNull Long userId,
    @RequestBody Map<String, String> params) {
    notifyService.send(templateCode, userId, params);
    return R.ok();
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-services/service-member,super-market-services/service-file,super-market-services/service-notify compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-member/.../MemberController.java \
        super-market-services/service-file/.../FileController.java \
        super-market-services/service-notify/.../NotifyController.java
git commit -m "feat: add deductPoints, download, and send endpoints"
```

---

### Task 12: B 组 — ProductController 更新端点

**Files:**
- Modify: `super-market-services/service-product/src/main/java/com/supermarket/product/controller/ProductController.java`
- Modify: `super-market-services/service-product/src/main/java/com/supermarket/product/service/ProductService.java`
- Modify: `super-market-services/service-product/src/main/java/com/supermarket/product/service/impl/ProductServiceImpl.java`

- [ ] **Step 1: ProductService 接口新增方法**

```java
// ProductService.java
void updateSpu(Long spuId, UpdateProductRequest request);
void updateSku(Long skuId, UpdateSkuRequest request);
```

- [ ] **Step 2: ProductController 新增端点**

```java
@PutMapping("/spu/{spuId}")
@Operation(summary = "更新商品SPU信息")
public R<Void> updateSpu(@PathVariable Long spuId, @Valid @RequestBody UpdateProductRequest request) {
    productService.updateSpu(spuId, request);
    return R.ok();
}

@PutMapping("/sku/{skuId}")
@Operation(summary = "更新SKU信息")
public R<Void> updateSku(@PathVariable Long skuId, @Valid @RequestBody UpdateSkuRequest request) {
    productService.updateSku(skuId, request);
    return R.ok();
}
```

- [ ] **Step 3: 新建 DTO**

Create: `super-market-services/service-product/src/main/java/com/supermarket/product/dto/UpdateProductRequest.java`
```java
package com.supermarket.product.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProductRequest {
    @Size(max = 200) private String name;
    @Size(max = 500) private String subtitle;
    private String mainImage;
    private String images;
    @Size(max = 2000) private String description;
}
```

Create: `super-market-services/service-product/src/main/java/com/supermarket/product/dto/UpdateSkuRequest.java`
```java
package com.supermarket.product.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateSkuRequest {
    @DecimalMin("0.01") private BigDecimal price;
    @DecimalMin("0") private BigDecimal marketPrice;
    private String image;
    private Integer status;
}
```

- [ ] **Step 4: ProductServiceImpl 实现**

```java
@Override
public void updateSpu(Long spuId, UpdateProductRequest request) {
    Spu spu = spuMapper.selectById(spuId);
    if (spu == null) throw new BizException(404, "商品不存在");
    if (request.getName() != null) spu.setName(request.getName());
    if (request.getSubtitle() != null) spu.setSubtitle(request.getSubtitle());
    if (request.getMainImage() != null) spu.setMainImage(request.getMainImage());
    if (request.getImages() != null) spu.setImages(request.getImages());
    if (request.getDescription() != null) spu.setDescription(request.getDescription());
    spuMapper.updateById(spu);
}

@Override
public void updateSku(Long skuId, UpdateSkuRequest request) {
    Sku sku = skuMapper.selectById(skuId);
    if (sku == null) throw new BizException(404, "SKU不存在");
    if (request.getPrice() != null) sku.setPrice(request.getPrice());
    if (request.getMarketPrice() != null) sku.setMarketPrice(request.getMarketPrice());
    if (request.getImage() != null) sku.setImage(request.getImage());
    if (request.getStatus() != null) sku.setStatus(request.getStatus());
    skuMapper.updateById(sku);
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn -pl super-market-services/service-product compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add super-market-services/service-product/
git commit -m "feat: add update SPU/SKU endpoints for product service"
```

---

### Task 13: 统一错误码体系

**Files:**
- Create: `super-market-common/common-core/src/main/java/com/supermarket/common/core/result/IResultCode.java`
- Create: `super-market-common/common-core/src/main/java/com/supermarket/common/core/result/ResultCode.java`
- Modify: `super-market-common/common-core/src/main/java/com/supermarket/common/core/exception/BizException.java`

- [ ] **Step 1: 新建 IResultCode 接口**

```java
package com.supermarket.common.core.result;

public interface IResultCode {
    int getCode();
    String getMessage();
}
```

- [ ] **Step 2: 新建 ResultCode 枚举**

```java
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
    ;

    private final int code;
    private final String message;
}
```

- [ ] **Step 3: BizException 新增枚举构造器**

```java
// BizException.java — 新增构造器
public BizException(ResultCode rc) {
    super(rc.getMessage());
    this.code = rc.getCode();
}
```

需要新增 import：`import com.supermarket.common.core.result.ResultCode;`

- [ ] **Step 4: 编译验证**

```bash
mvn -pl super-market-common/common-core compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-core/src/main/java/com/supermarket/common/core/result/IResultCode.java \
        super-market-common/common-core/src/main/java/com/supermarket/common/core/result/ResultCode.java \
        super-market-common/common-core/src/main/java/com/supermarket/common/core/exception/BizException.java
git commit -m "feat: add unified error code enum (ResultCode + IResultCode)"
```

---

### Task 14: 前端测试基础 — packages/utils + packages/stores

**Files:**
- Create: `frontend/packages/utils/src/__tests__/format.test.ts`
- Create: `frontend/packages/stores/src/__tests__/useUserStore.test.ts`
- Modify: `frontend/packages/utils/package.json`（无 Vitest 则安装）

- [ ] **Step 1: 安装 Vitest**

```bash
cd frontend && pnpm add -D vitest @vue/test-utils jsdom -w
```

- [ ] **Step 2: 新建 format.test.ts**

```typescript
// frontend/packages/utils/src/__tests__/format.test.ts
import { describe, it, expect } from 'vitest'
import { formatPrice, formatDate, formatCountdown } from '../format'

describe('formatPrice', () => {
  it('formats integer price', () => {
    expect(formatPrice(99)).toBe('99.00')
  })

  it('formats decimal price', () => {
    expect(formatPrice(99.9)).toBe('99.90')
  })

  it('formats large price with commas', () => {
    expect(formatPrice(12345.67)).toBe('12,345.67')
  })
})

describe('formatDate', () => {
  it('formats date string', () => {
    expect(formatDate('2026-01-15 10:30:00')).toMatch(/2026/)
  })
})

describe('formatCountdown', () => {
  it('formats countdown from seconds', () => {
    const result = formatCountdown(3661)
    expect(result).toContain('01')
    expect(result).toContain('01')
    expect(result).toContain('01')
  })
})
```

- [ ] **Step 3: 新建 useUserStore.test.ts**

```typescript
// frontend/packages/stores/src/__tests__/useUserStore.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../useUserStore'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('starts unauthenticated', () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
  })

  it('sets token and userInfo on login', () => {
    const store = useUserStore()
    store.login('test-token', { id: 1, nickname: 'Test' } as any)
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('test-token')
  })

  it('clears state on logout', () => {
    const store = useUserStore()
    store.login('test-token', { id: 1 } as any)
    store.logout()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
  })
})
```

- [ ] **Step 4: 运行测试**

```bash
cd frontend && npx vitest run packages/utils packages/stores
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd D:/MyWorkStation/Java/program/super-market
git add frontend/
git commit -m "test: add Vitest unit tests for utils and stores packages"
```

---

## 第 3~4 周 · P1 前端补全

### Task 15: B2C 路由级联 — AddressPage 补全（约第 3 周前段）

**Files:**
- Modify: `frontend/app-b2c/src/pages/user/AddressPage.vue`

当前状态：34 行，仅展示地址列表，无增删改 UI。

目标：~220 行，包含新增/编辑对话框、删除确认、设为默认按钮。

重构 AddressPage.vue：

```vue
<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressApi } from '@supermarket/api'
import { useAddressStore } from '@supermarket/stores'
import type { Address } from '@supermarket/api'

const store = useAddressStore()
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = computed(() => editingId.value ? '编辑地址' : '新增地址')

const editingId = ref<number | null>(null)
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  label: '',
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  district: [{ required: true, message: '请选择区县', trigger: 'change' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

onMounted(() => { store.fetchAddresses() })

function openAdd() {
  editingId.value = null
  Object.assign(form, { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', label: '' })
  dialogVisible.value = true
}

function openEdit(addr: Address) {
  editingId.value = addr.id
  Object.assign(form, {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detailAddress: addr.detailAddress,
    label: addr.label || '',
  })
  dialogVisible.value = true
}

async function submit(formEl: any) {
  if (!formEl) return
  await formEl.validate()
  loading.value = true
  try {
    if (editingId.value) {
      await addressApi.updateAddress(editingId.value, { ...form })
      ElMessage.success('地址已更新')
    } else {
      await addressApi.createAddress({ ...form })
      ElMessage.success('地址已添加')
    }
    dialogVisible.value = false
    await store.fetchAddresses()
  } finally {
    loading.value = false
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该地址？', '确认删除', { type: 'warning' })
  await addressApi.deleteAddress(id)
  ElMessage.success('已删除')
  await store.fetchAddresses()
}

async function setDefault(id: number) {
  await addressApi.setDefault(id)
  ElMessage.success('已设为默认地址')
  await store.fetchAddresses()
}
</script>

<template>
  <div class="address-page">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openAdd">新增地址</el-button>
    </div>

    <el-empty v-if="store.addresses.length === 0" description="暂无地址" />

    <div v-for="addr in store.addresses" :key="addr.id" class="address-card" :class="{ default: addr.isDefault }">
      <div class="addr-header">
        <span class="receiver">{{ addr.receiverName }} {{ addr.receiverPhone }}</span>
        <el-tag v-if="addr.isDefault" type="danger" size="small">默认</el-tag>
        <el-tag v-if="addr.label" size="small">{{ addr.label }}</el-tag>
      </div>
      <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddress }}</p>
      <div class="addr-actions">
        <el-button text type="primary" @click="openEdit(addr)">编辑</el-button>
        <el-button text type="danger" @click="remove(addr.id)">删除</el-button>
        <el-button v-if="!addr.isDefault" text type="primary" @click="setDefault(addr.id)">设为默认</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" maxlength="11" />
        </el-form-item>
        <el-form-item label="省/市/区" required>
          <el-row :gutter="8">
            <el-col :span="8"><el-input v-model="form.province" placeholder="省" /></el-col>
            <el-col :span="8"><el-input v-model="form.city" placeholder="市" /></el-col>
            <el-col :span="8"><el-input v-model="form.district" placeholder="区" /></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" maxlength="200" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.label" placeholder="如：家、公司" maxlength="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submit($refs.formRef)">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.address-card { border: 1px solid #dcdfe6; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.address-card.default { border-color: #f56c6c; }
.addr-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.receiver { font-weight: 600; font-size: 15px; }
.addr-detail { color: #606266; margin: 0 0 12px 0; }
.addr-actions { display: flex; gap: 8px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
```

- [ ] **Step 1: 开发服务器验证**

```bash
cd frontend && pnpm run dev:b2c
```

打开浏览器访问 B2C，登录后导航到 `/user/addresses`，验证：新增、编辑、删除、设为默认地址。

- [ ] **Step 2: Commit**

```bash
git add frontend/app-b2c/src/pages/user/AddressPage.vue
git commit -m "feat: complete B2C AddressPage with CRUD and default address"
```

---

### Task 16~20: 其余 B2C 页面补全（OrderDetail / Orders / OrderResult / Coupons / Center）

由于篇幅，此处列出每个页面的关键组件和数据流。实际实施时各页面为独立 Task。

| Task | 页面 | 核心改动 |
|------|------|---------|
| 16 | OrderDetailPage (`/order/:no`) | `useOrderStore.fetchDetail(orderNo)` → 商品明细 + 物流时间线 + `Countdown` 支付倒计时 + 取消/确认收货按钮 |
| 17 | OrdersPage (`/user/orders`) | `useOrderStore.fetchList(status, page)` → `el-tabs` 状态筛选 + `el-pagination` + 操作按钮 |
| 18 | OrderResultPage (`/order/result`) | 从 URL query 读取 `orderNo`，调用 `useOrderStore.fetchDetail()` 显示支付状态，提供返回首页/查看订单按钮 |
| 19 | CouponPage (`/user/coupons`) | `useCouponStore`，两个 Tab：领券中心（`fetchAvailable` + `claim`）+ 我的券（`fetchMy`，按 status 筛选） |
| 20 | CenterPage (`/user/center`) | 用户信息编辑表单 + 会员等级卡片（调用 `memberApi.getMember`）+ 快捷入口 |

- [ ] **Step 1: 逐页面补全**

每完成一个页面：
```bash
cd frontend && pnpm run dev:b2c  # 先验证功能
git add frontend/app-b2c/src/pages/<page-dir>/
git commit -m "feat: complete B2C <PageName>"
```

最后三个（seckill/reviews/notifications）后续按同一模式补全。

---

### Task 21: Pinia Store 新增（4 个）

**Files:**
- Create: `frontend/packages/stores/src/useAddressStore.ts`
- Create: `frontend/packages/stores/src/useOrderStore.ts`
- Create: `frontend/packages/stores/src/useCouponStore.ts`
- Create: `frontend/packages/stores/src/useNotifyStore.ts`

- [ ] **Step 1: useAddressStore.ts**

```typescript
// frontend/packages/stores/src/useAddressStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { addressApi } from '@supermarket/api'
import type { Address } from '@supermarket/api'

export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)

  async function fetchAddresses() {
    loading.value = true
    try { addresses.value = await addressApi.getAddresses() }
    finally { loading.value = false }
  }

  const defaultAddress = computed(() => addresses.value.find(a => a.isDefault))

  return { addresses, loading, fetchAddresses, defaultAddress }
})
```

其他 3 个 Store 遵循同样模式：
- `useOrderStore`: `orders[]`, `currentOrder`, `fetchList()`, `fetchDetail()`
- `useCouponStore`: `availableCoupons[]`, `myCoupons[]`, `fetchAvailable()`, `fetchMy()`, `claim()`
- `useNotifyStore`: `notifications[]`, `unreadCount`, `fetchList()`, `markRead()`, `markAllRead()`

- [ ] **Step 2: 编译验证**

```bash
cd frontend && npx vue-tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/stores/src/
git commit -m "feat: add useAddressStore, useOrderStore, useCouponStore, useNotifyStore"
```

---

### Task 22: 前端 UI 组件测试 + B2C 页面测试

**Files:**
- Create: `frontend/packages/ui/src/__tests__/ProductCard.test.ts`
- Create: `frontend/packages/ui/src/__tests__/SearchBar.test.ts`
- Create: `frontend/packages/ui/src/__tests__/SkuSelector.test.ts`
- Create: `frontend/app-b2c/src/pages/home/__tests__/HomePage.test.ts`

- [ ] **Step 1: ProductCard 组件测试**

```typescript
// packages/ui/src/__tests__/ProductCard.test.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductCard from '../ProductCard.vue'

describe('ProductCard', () => {
  it('renders product name and price', () => {
    const wrapper = mount(ProductCard, {
      props: {
        product: {
          spuId: 1,
          name: '测试商品',
          minPrice: 99.00,
          mainImage: '/test.jpg',
          salesCount: 100,
        }
      }
    })
    expect(wrapper.text()).toContain('测试商品')
    expect(wrapper.text()).toContain('99')
  })
})
```

- [ ] **Step 2: 运行全部前端测试**

```bash
cd frontend && npx vitest run
```

Expected: ALL PASS (utils + stores + ui components)

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/ui/src/__tests__/ frontend/app-b2c/src/pages/home/__tests__/
git commit -m "test: add Vitest component tests for UI kit and B2C pages"
```

---

## 第 5~6 周 · P2 生产就绪

### Task 23: Actuator 接入（18 个服务 + Gateway）

**Files:**
- Modify: 18 个 `super-market-services/service-*/pom.xml`
- Modify: 18 个 `super-market-services/service-*/src/main/resources/application.yml`（加 management 配置）
- Verify: Gateway 已有 actuator

- [ ] **Step 1: 各服务 pom.xml 加 actuator 依赖**

```xml
<!-- 在每个 service pom.xml 的 dependencies 块中加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

- [ ] **Step 2: 各服务 application.yml 加 management 配置**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

- [ ] **Step 3: 验证**

启动任一服务后：
```bash
curl http://localhost:9301/actuator/health
```

Expected: `{"status":"UP"}`

- [ ] **Step 4: Commit**

```bash
git add super-market-services/*/pom.xml super-market-services/*/src/main/resources/application.yml
git commit -m "feat: add Spring Boot Actuator to all 18 services"
```

---

### Task 24: Micrometer/Prometheus 指标接入

**Files:**
- Modify: 18 个服务的 `pom.xml` + `application.yml`
- Modify: `docker-compose/prometheus/prometheus.yml`

- [ ] **Step 1: 各服务加 micrometer-registry-prometheus 依赖**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

- [ ] **Step 2: 各服务 application.yml 加 prometheus 端点**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

- [ ] **Step 3: prometheus.yml 取消注释并配置 scrape**

```yaml
# docker-compose/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'spring-boot-apps'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'host.docker.internal:9301'  # service-user
        - 'host.docker.internal:9302'  # service-auth
        # ... 其余 16 个服务端口
```

- [ ] **Step 4: 验证**

```bash
cd docker-compose && docker compose up -d prometheus
curl http://localhost:9301/actuator/prometheus | head -5
```

Expected: 有 jvm_ 和 http_server_requests_ 等指标输出

- [ ] **Step 5: Commit**

```bash
git add super-market-services/ docker-compose/prometheus/prometheus.yml
git commit -m "feat: add Micrometer Prometheus metrics to all services"
```

---

### Task 25: SkyWalking Agent 开发环境 Volume 挂载

**Files:**
- Modify: `docker-compose/docker-compose.yml`（加 volume mount）
- Create: `docker-compose/skywalking/agent/` 目录（从 SkyWalking 官方下载 agent jar）

- [ ] **Step 1: .env 加 SKYWALKING_AGENT 路径变量**

```bash
# .env
SKYWALKING_AGENT_DIR=./skywalking/agent
```

- [ ] **Step 2: 验证**

```bash
# 启动 SkyWalking + 一个服务
cd docker-compose && docker compose up -d skywalking-oap skywalking-ui
# 启动服务时设 JAVA_TOOL_OPTIONS
JAVA_TOOL_OPTIONS="-javaagent:./docker-compose/skywalking/agent/skywalking-agent.jar -DSW_AGENT_NAME=service-user -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800" \
  mvn -pl super-market-services/service-user spring-boot:run
```

打开 `http://localhost:18083` 确认 service-user 出现在 SkyWalking UI 拓扑中。

- [ ] **Step 3: Commit**

```bash
git add docker-compose/skywalking/ docker-compose/.env.example .gitignore
git commit -m "feat: add SkyWalking agent volume mount for dev environment"
```

---

### Task 26: Grafana 仪表盘预置

**Files:**
- Create: `docker-compose/grafana/provisioning/dashboards/dashboard-provider.yaml`
- Create: `docker-compose/grafana/provisioning/dashboards/jvm-overview.json`
- Create: `docker-compose/grafana/provisioning/dashboards/dubbo-rpc.json`
- Create: `docker-compose/grafana/provisioning/dashboards/business-qps.json`
- Create: `docker-compose/grafana/provisioning/dashboards/error-top10.json`

- [ ] **Step 1: dashboard-provider.yaml**

```yaml
apiVersion: 1
providers:
  - name: 'super-market'
    orgId: 1
    folder: 'Super Market'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/provisioning/dashboards
```

- [ ] **Step 2: 4 个仪表盘 JSON**

**jvm-overview.json**（JVM 堆内存 + GC 暂停时间）:

```json
{
  "dashboard": {
    "title": "JVM Overview",
    "panels": [
      {
        "title": "Heap Memory Used",
        "type": "graph",
        "targets": [
          { "expr": "jvm_memory_used_bytes{area=\"heap\"}", "legendFormat": "{{service}}" }
        ]
      },
      {
        "title": "GC Pause Duration",
        "type": "graph",
        "targets": [
          { "expr": "rate(jvm_gc_pause_seconds_sum[1m]) / rate(jvm_gc_pause_seconds_count[1m])", "legendFormat": "{{service}}" }
        ]
      },
      {
        "title": "Thread Count",
        "type": "stat",
        "targets": [
          { "expr": "jvm_threads_live_threads", "legendFormat": "{{service}}" }
        ]
      }
    ]
  }
}
```

**dubbo-rpc.json**（Dubbo provider QPS + 延迟）:

```json
{
  "dashboard": {
    "title": "Dubbo RPC Metrics",
    "panels": [
      {
        "title": "Provider QPS",
        "type": "graph",
        "targets": [
          { "expr": "rate(dubbo_provider_success_total[1m])", "legendFormat": "{{service}}.{{method}}" }
        ]
      },
      {
        "title": "Provider Response Time (P99)",
        "type": "graph",
        "targets": [
          { "expr": "histogram_quantile(0.99, rate(dubbo_provider_rt_seconds_bucket[1m]))", "legendFormat": "{{service}}.{{method}}" }
        ]
      },
      {
        "title": "Provider Error Rate",
        "type": "graph",
        "targets": [
          { "expr": "rate(dubbo_provider_total[1m]) - rate(dubbo_provider_success_total[1m])", "legendFormat": "{{service}}" }
        ]
      }
    ]
  }
}
```

**business-qps.json**（业务 QPS 按服务+接口）:

```json
{
  "dashboard": {
    "title": "Business QPS",
    "panels": [
      {
        "title": "HTTP QPS by Service",
        "type": "graph",
        "targets": [
          { "expr": "rate(http_server_requests_seconds_count[1m])", "legendFormat": "{{service}} - {{uri}}" }
        ]
      },
      {
        "title": "P99 Latency by Endpoint",
        "type": "graph",
        "targets": [
          { "expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[1m]))", "legendFormat": "{{service}} - {{uri}}" }
        ]
      }
    ]
  }
}
```

**error-top10.json**（错误率排名）:

```json
{
  "dashboard": {
    "title": "Error Rate TOP 10",
    "panels": [
      {
        "title": "5xx Error Rate by Endpoint",
        "type": "bargauge",
        "targets": [
          { "expr": "topk(10, rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]))", "legendFormat": "{{service}} - {{uri}}" }
        ]
      },
      {
        "title": "Error Log Count",
        "type": "stat",
        "targets": [
          { "expr": "logback_events_total{level=\"error\"}", "legendFormat": "{{service}}" }
        ]
      }
    ]
  }
}
```

（部署前通过 Grafana UI 验证并微调面板样式，以上为 query 定义。）

- [ ] **Step 3: 验证**

```bash
cd docker-compose && docker compose restart grafana
# 打开 http://localhost:3000，确认 4 个仪表盘出现在 Super Market 文件夹中
```

- [ ] **Step 4: Commit**

```bash
git add docker-compose/grafana/provisioning/dashboards/
git commit -m "feat: add 4 pre-built Grafana dashboards"
```

---

### Task 27: K8s Service × 19 + Ingress + HPA

**Files:**
- Create: `super-market-k8s/apps/service-user.yaml` 等 19 个 Service
- Create: `super-market-k8s/base/ingress.yaml`
- Create: `super-market-k8s/apps/hpa-gateway.yaml` 等 5 个 HPA

- [ ] **Step 1: 生成 Service YAML（以 service-user 为例）**

```yaml
# super-market-k8s/apps/service-user.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: super-market
spec:
  selector:
    app: user-service
  ports:
    - name: dubbo
      port: 9301
      targetPort: 9301
      protocol: TCP
  type: ClusterIP
```

其他 18 个服务同理。Gateway Service 类型为 ClusterIP，端口 8999。

- [ ] **Step 2: 新建 Ingress**

```yaml
# super-market-k8s/base/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: super-market-ingress
  namespace: super-market
spec:
  ingressClassName: nginx
  rules:
    - host: api.super-market.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway-service
                port:
                  number: 8999
```

- [ ] **Step 3: 新建 HPA（以 gateway 为例）**

```yaml
# super-market-k8s/apps/hpa-gateway.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gateway-hpa
  namespace: super-market
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

其余 4 个 HPA 同理（user-service / order-service / product-service / payment-service）。

- [ ] **Step 4: Commit**

```bash
git add super-market-k8s/apps/service-*.yaml super-market-k8s/base/ingress.yaml super-market-k8s/apps/hpa-*.yaml
git commit -m "feat: add K8s Service x19, Ingress, HPA x5"
```

---

### Task 28: K8s StatefulSet (MySQL + Redis) + NetworkPolicy

**Files:**
- Create: `super-market-k8s/statefulset/mysql.yaml`
- Create: `super-market-k8s/statefulset/redis.yaml`
- Create: `super-market-k8s/base/network-policy.yaml`

- [ ] **Step 1: MySQL StatefulSet（简化版，1 主）**

```yaml
# super-market-k8s/statefulset/mysql.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
  namespace: super-market
spec:
  serviceName: mysql-headless
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0.35
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: super-market-secrets
                  key: MYSQL_ROOT_PASSWORD
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: mysql-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 50Gi
```

- [ ] **Step 2: NetworkPolicy（最小权限）**

```yaml
# super-market-k8s/base/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny
  namespace: super-market
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: ingress-nginx
      ports:
        - port: 8999
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dubbo
  namespace: super-market
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector: {}
      ports:
        - port: 9301  # user
        - port: 9311  # product
        - port: 9322  # order
        - port: 9331  # payment
        # ... 其余服务端口
```

- [ ] **Step 3: Commit**

```bash
git add super-market-k8s/statefulset/ super-market-k8s/base/network-policy.yaml
git commit -m "feat: add K8s StatefulSet (MySQL, Redis) and NetworkPolicy"
```

---

### Task 29: CI/CD 并行构建 + 安全扫描 + 优雅关闭

**Files:**
- Modify: `.gitlab-ci.yml`
- Modify: 20 个 `Dockerfile`

- [ ] **Step 1: .gitlab-ci.yml build 阶段改为 parallel:matrix**

```yaml
docker-build:
  stage: build
  image: docker:24-dind
  parallel:
    matrix:
      - SERVICE:
          - service-user
          - service-auth
          - service-member
          - service-address
          - service-product
          - service-category
          - service-inventory
          - service-review
          - service-cart
          - service-order
          - service-payment
          - service-coupon
          - service-seckill
          - service-search
          - service-shop
          - service-platform
          - service-file
          - service-notify
          - gateway
  script:
    - cd super-market-services/$SERVICE 2>/dev/null || cd super-market-gateway
    - docker build -t $HARBOR_URL/$HARBOR_PROJECT/$SERVICE:$CI_COMMIT_SHORT_SHA .
    - docker push $HARBOR_URL/$HARBOR_PROJECT/$SERVICE:$CI_COMMIT_SHORT_SHA
  when: manual
```

- [ ] **Step 2: 新增 security 阶段**

```yaml
trivy-scan:
  stage: security
  image: aquasec/trivy:latest
  script:
    - trivy image --severity HIGH,CRITICAL $HARBOR_URL/$HARBOR_PROJECT/$SERVICE:$CI_COMMIT_SHORT_SHA
  allow_failure: true
  when: manual
```

- [ ] **Step 3: 20 个 Dockerfile 加优雅关闭参数**

```dockerfile
# 每个 Dockerfile 的 ENTRYPOINT 改前:
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
# 改后:
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.lifecycle.timeout-per-shutdown-phase=30s", "-jar", "/app/app.jar"]
```

网关的 Dockerfile 允许 45s 关闭（路由处理时间更长）。

- [ ] **Step 4: 验证**

```bash
# 验证 gitlab-ci.yml 语法
gitlab-ci-lint .gitlab-ci.yml
```

- [ ] **Step 5: Commit**

```bash
git add .gitlab-ci.yml super-market-services/*/Dockerfile super-market-gateway/Dockerfile
git commit -m "feat: parallel CI build, security scan, graceful shutdown for all services"
```

---

## 验证检查清单

完成全部 Task 后执行：

- [ ] **编译全量通过**
  ```bash
  mvn clean compile -DskipTests
  ```

- [ ] **所有测试通过**
  ```bash
  mvn test
  cd frontend && npx vitest run
  ```

- [ ] **Gateway 路由 + RBAC 验证**
  ```bash
  # 无 Token 访问管理接口 → 401
  curl -i http://localhost:8999/api/platform/admin/banners
  # 用户 Token 访问管理接口 → 403
  curl -i -H "Authorization: Bearer <user_token>" http://localhost:8999/api/platform/admin/banners
  ```

- [ ] **输入校验验证**
  ```bash
  # 短密码 → 400 + "密码长度为6-32位"
  curl -X POST http://localhost:8999/api/auth/login -d "phone=13800138000&password=12"
  ```

- [ ] **K8s 清单验证**
  ```bash
  kubectl --dry-run=client apply -f super-market-k8s/
  ```

---

## 工作量估算

| 阶段 | 人天 |
|------|------|
| P0 安全底线（Task 1-8） | 5~7 |
| P1 服务端堵漏（Task 9-14） | 5~7 |
| P1 前端补全（Task 15-22） | 8~12 |
| P2 生产就绪（Task 23-29） | 8~12 |
| **合计** | **26~38** |
