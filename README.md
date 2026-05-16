<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen?logo=springboot" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/Dubbo-3.2.13-blue?logo=apache" alt="Dubbo 3.2.13">
  <img src="https://img.shields.io/badge/Vue-3.4-4fc08d?logo=vuedotjs" alt="Vue 3.4">
  <img src="https://img.shields.io/badge/pnpm-9.x-f69220?logo=pnpm" alt="pnpm 9">
  <img src="https://img.shields.io/badge/K8s-ready-326ce5?logo=kubernetes" alt="Kubernetes Ready">
</p>

<h1 align="center">Super Market</h1>
<p align="center"><strong>京东式全栈电商平台 &middot; 18 个微服务 &middot; 100% 本地可运行</strong></p>

---

## 项目简介

Super Market 是一个**生产级全栈电商系统**，从商品浏览到下单支付、从商家入驻到运营审核，完整覆盖电商全链路。系统采用 **DDD 领域驱动设计**，按限界上下文拆分为 18 个独立微服务，**全部可本地一键启动**，无需任何云服务依赖。

> 适合：微服务架构学习、全栈项目实战、电商系统二次开发、技术面试作品集

## 核心特性

### 后端 — 企业级微服务架构

- **18 个微服务**，按业务域垂直拆分：用户、商品、交易、支付、营销、搜索、商家、平台、基础支撑
- **Dubbo 3.x Triple 协议** RPC 通信，高性能二进制序列化，服务注册发现基于 Nacos
- **Spring Cloud Gateway** 统一入口，JWT 鉴权 + 路径路由 + 请求转发
- **MySQL 垂直分库**（每服务独立数据库）+ ShardingSphere-JDBC 水平分表（大表场景）
- **分布式事务**：RocketMQ 事务消息保证最终一致性；**秒杀**：Redis + Lua 脚本实现原子扣减
- **全链路可观测**：SkyWalking 链路追踪 + Prometheus 指标采集

### 前端 — 三端分离 pnpm Monorepo

| 应用 | 色系 | 页面 | 受众 |
|------|------|------|------|
| `app-b2c` 用户前台 | 京东红 #e1251b | 15 页 | 消费者 |
| `app-b2b` 商家后台 | 商务蓝 #3b82f6 | 12 页 | 商家 |
| `app-admin` 运营后台 | 紫晶 #8b5cf6 | 8 页 | 运营人员 |

- **Vue 3 + TypeScript + Element Plus**，Composition API 全部使用 `<script setup>`
- **pnpm workspace** 管理共享包：`@supermarket/ui`（10 组件）、`@supermarket/api`（15 模块）、`@supermarket/stores`（Pinia）、`@supermarket/utils`
- Axios 封装 Token 注入 + 401 自动跳转，开箱即用

### 开发体验

- **Docker Compose 一键启动中间件**：MySQL、Redis、Nacos、RocketMQ、Elasticsearch、MinIO、Sentinel
- **零外部依赖**：不依赖任何公有云服务，100% 本地可运行
- **生产级 K8s 部署配置**已就绪（Deployment、ConfigMap、Secrets、Namespace）
- Swagger/Knife4j 在线 API 文档，网关统一入口 `localhost:8999/doc.html`

## 技术栈

| 领域 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 21, TypeScript 5.3 | — |
| 框架 | Spring Boot 3.2.5, Dubbo 3.2.13 | Triple 协议 |
| 网关 | Spring Cloud Gateway 4.1 | JWT 鉴权 + 路由分发 |
| 前端 | Vue 3.4, Vite 5, Element Plus 2.6 | pnpm monorepo |
| 状态 | Pinia 2.1, Vue Router 4.3 | — |
| 数据库 | MySQL 8, MyBatis-Plus, ShardingSphere-JDBC | 垂直分库 + 水平分表 |
| 缓存 | Redis 7 | Caffeine 本地缓存 + Redis 分布式缓存 |
| 消息 | RocketMQ 5 | 事务消息、异步解耦 |
| 搜索 | Elasticsearch 8 | 商品全文检索 |
| 存储 | MinIO | 图片/文件对象存储 |
| 注册中心 | Nacos 2 | 服务发现 + 配置管理 |
| 可观测 | SkyWalking, Prometheus | 链路追踪 + 指标监控 |
| 治理 | Sentinel | 熔断降级 + 限流 |
| 容器 | Docker Compose（开发）, Kubernetes（生产） | — |

## 快速开始

### 前置要求

- JDK 21+, Maven 3.8+, Node.js 18+, pnpm 9+
- Docker Desktop

### 1. 启动中间件

```bash
cd middleware-docker
docker compose up -d
```

一键启动 MySQL、Redis、Nacos (:8848)、RocketMQ、Elasticsearch (:9200)、MinIO (:9000)。

### 2. 启动后端

```bash
# 编译全部模块（跳过测试，加快构建）
mvn clean install -DskipTests

# 启动网关
mvn -pl super-market-gateway spring-boot:run

# 启动所需服务（按需启动，每服一个终端）
mvn -pl super-market-services/service-user spring-boot:run
mvn -pl super-market-services/service-product spring-boot:run
mvn -pl super-market-services/service-order spring-boot:run
# ... 依次启动其他服务
```

### 3. 启动前端

```bash
cd frontend

# 安装依赖
pnpm install

# 同时启动三个应用
pnpm dev:b2c    # 用户前台 → http://localhost:5173
pnpm dev:b2b    # 商家后台 → http://localhost:5174
pnpm dev:admin  # 运营后台 → http://localhost:5175
```

> 所有 HTTP 请求经网关 `localhost:8999` 统一入口，前端 Vite 已配置代理。

### 4. 访问 API 文档

打开浏览器访问 **http://localhost:8999/doc.html** 查看全部 API。

## 项目结构

```
super-market/
├── super-market-common/           # 共享库
│   ├── common-core/               #   R<T> 响应体、BizException、常量
│   ├── common-dubbo-api/          #   Dubbo 服务接口定义（跨服务契约）
│   ├── common-security/           #   JWT 工具、RBAC 配置
│   ├── common-web/                #   全局异常处理、Web 配置
│   └── common-mybatis/            #   MyBatis-Plus 基础 Entity、分页
├── super-market-gateway/          # Spring Cloud Gateway (:8999)
├── super-market-services/         # 18 个微服务
│   ├── service-user (:9301)       #   用户服务
│   ├── service-auth (:9302)       #   认证授权
│   ├── service-member (:9303)     #   会员服务
│   ├── service-address (:9304)    #   地址服务
│   ├── service-product (:9311)    #   商品服务
│   ├── service-category (:9312)   #   类目服务
│   ├── service-inventory (:9313)  #   库存服务
│   ├── service-review (:9314)     #   评价服务
│   ├── service-cart (:9321)       #   购物车服务
│   ├── service-order (:9322)      #   订单服务
│   ├── service-payment (:9331)    #   支付服务
│   ├── service-coupon (:9351)     #   优惠券服务
│   ├── service-seckill (:9352)    #   秒杀服务
│   ├── service-search (:9361)     #   搜索服务
│   ├── service-shop (:9341)       #   店铺服务
│   ├── service-platform (:9381)   #   平台运营
│   ├── service-file (:9371)       #   文件服务
│   └── service-notify (:9372)     #   通知服务
├── frontend/                      # 前端 pnpm Monorepo
│   ├── packages/                  #   共享包
│   │   ├── ui/                    #     10 个通用组件
│   │   ├── api/                   #     15 个 API 模块
│   │   ├── stores/                #     Pinia 状态
│   │   └── utils/                 #     工具函数
│   ├── app-b2c/                   #   用户前台 (15 页)
│   ├── app-b2b/                   #   商家后台 (12 页)
│   └── app-admin/                 #   运营后台 (8 页)
├── super-market-k8s/              # Kubernetes 部署配置
├── middleware-docker/                # 开发环境中间件
└── docs/                          # PRD · 架构设计 · 方案文档
```

## 关键设计决策

**为什么用 Dubbo 而不是 Spring Cloud 的 HTTP 调用？**
Dubbo Triple 协议基于 gRPC，二进制序列化比 JSON 体积更小、速度更快。在服务间高频调用（如库存扣减、价格查询）场景下，延迟优势明显。

**为什么每个服务独立数据库？**
垂直分库避免单点瓶颈，服务间通过 RPC 通信而非直接查表，确保数据边界清晰。大表（订单、商品）再叠加 ShardingSphere 水平分表。

**为什么要三套前端而不是一个后台切角色？**
三端面向完全不同的用户群体和使用场景——消费者逛商城、商家管店铺、运营审核控风险。独立构建部署避免权限泄露，也方便团队并行开发。

## 贡献指南

本项目为个人学习项目，暂不接受外部 PR。欢迎 Star & Fork 用于学习！

## License

MIT © 2026 sunachao
