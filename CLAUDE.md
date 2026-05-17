# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules (skip tests)
mvn clean install -DskipTests

# Run tests for a single module
mvn -pl super-market-services/service-user test

# Run a single test class
mvn -pl super-market-services/service-user test -Dtest=UserControllerTest

# CheckStyle
mvn checkstyle:check
```

## Architecture Overview

**Stack:** Java 21, Spring Boot 3.2.5, Dubbo 3.2.13 (Triple protocol), Spring Cloud Gateway 4.1

### Module Layout

```
super-market-common/          # Shared libraries (5 modules)
  common-core/                # R<T> response, BizException, GlobalConstants
  common-dubbo-api/           # Dubbo service interface definitions (shared between services)
  common-security/            # JWT utilities, RBAC config
  common-web/                 # Global exception handler, web config
  common-mybatis/             # MyBatis-Plus base entity, pagination, MetaObjectHandler

super-market-gateway/         # Spring Cloud Gateway — the single entry point on port 8999
  filter/AuthGlobalFilter.java  # JWT validation filter (whitelist: /api/user/register, /api/user/login, /api/auth/login, /actuator, /doc.html, /v3/api-docs)

super-market-services/        # 18 microservice modules
  service-user (9301)         service-auth (9302)        service-member (9303)
  service-address (9304)      service-product (9311)     service-category (9312)
  service-inventory (9313)    service-review (9314)      service-cart (9321)
  service-order (9322)        service-payment (9331)     service-coupon (9351)
  service-seckill (9352)      service-search (9361)      service-shop (9341)
  service-platform (9381)     service-file (9371)        service-notify (9372)

super-market-k8s/             # Kubernetes deploy configs (Deployment templates, ConfigMap, Secrets, Namespace)
middleware-docker/               # Dev middleware: MySQL, Redis, Nacos, RocketMQ, ES, MinIO, Sentinel, SkyWalking, Canal
scripts/                      # Service mgmt with health+Nacos verification: start-all, start-gateway, start-service, start-frontend, stop-all, stop-frontend
docs/                         # PRD, solution design, implementation plans
```

### Key Patterns

- **Inter-service calls:** Services communicate via Dubbo 3.x Triple protocol. API interfaces live in `common-dubbo-api`. Service implementations register as Dubbo providers (`@EnableDubbo` + `application-dev.yml` with `dubbo.protocol.name: tri`, registry `nacos://...?group=dubbo`).
- **Gateway routing:** Spring Cloud Gateway routes HTTP requests by path prefix (e.g., `/api/user/**` → `user-service`). The gateway validates JWT tokens via `AuthGlobalFilter` and forwards `X-User-Id` and `X-User-Roles` headers to downstream services.
- **Service bootstrap:** Each service's `Application` class uses `scanBasePackages = {"com.supermarket.<service>", "com.supermarket.common"}` to pick up common module beans.
- **Unified response:** All REST endpoints return `R<T>` (code, message, data, timestamp) from `common-core`. Business exceptions return HTTP 200 — check the `code` field for error info. Error codes defined in `ResultCode` enum (`common-core/src/.../result/ResultCode.java`): SYSTEM 9xxxx, USER 1xxxx, PRODUCT 2xxxx, ORDER 3xxxx, MARKETING 4xxxx, SHOP 5xxxx, PLATFORM 6xxxx, FILE 7xxxx.
- **Configuration:** Each service has 2 config files (no `application.yml`):
  - `bootstrap.yml` — service identity, Nacos discovery/config, shared-configs (common-redis/rocketmq/seata), extension-configs (rate-limit/gray-release)
  - `application-dev.yml` — datasource, Redis, Dubbo, MyBatis-Plus, logging, springdoc, management
  - Middleware host/port/credentials via `${ENV_VAR:default}` from `.env`; profile via `${SPRING_PROFILES_ACTIVE:dev}`; `spring-cloud-starter-bootstrap` inherited from parent POM
- **Database:** Each service has its own database (vertical sharding). MyBatis-Plus with `assign_id` ID generation. Large tables (orders, products) use horizontal sharding with ShardingSphere-JDBC.
- **Service port ranges:** User domain 9301-9304, Product domain 9311-9314, Order domain 9321-9322, Payment 9331, Shop 9341, Marketing 9351-9352, Search 9361, File/Notify 9371-9372, Platform 9381.
- **Admin account:** `13800000000 123456`
### Environment Variables

All middleware host/port/credentials in microservice YAML configs use `${VAR:default}` placeholders:

| Variable | Default | Used by |
|---|---|---|
| `DOCKER_HOST_IP` | `localhost` | All middleware host connections |
| `NACOS_PORT` | `8848` | Nacos discovery, config, Dubbo registry |
| `NACOS_USERNAME` | `nacos` | Nacos auth |
| `NACOS_PASSWORD` | `nacos` | Nacos auth |
| `MYSQL_PORT` | `3306` | MySQL datasource |
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL datasource |
| `REDIS_PORT` | `6379` | Redis |
| `REDIS_PASSWORD` | `redis123` | Redis |
| `ROCKETMQ_NAMESRV_PORT` | `9876` | RocketMQ name server |
| `ES01_PORT` | `9200` | Elasticsearch |
| `SEATA_PORT` | `8091` | Seata distributed transaction |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (dev/test/prod) |
| `SERVER_PORT` | per-service | Override default service port |

`middleware-docker/.env.example` documents all variables with standard ports. `middleware-docker/.env` is the actual config (gitignored, may use custom ports to avoid local conflicts). Scripts in `scripts/` auto-source `.env` and export all variables with defaults.

### Developer Workflow

> Middleware (MySQL, Redis, Nacos, RocketMQ, ES, etc.) is assumed already running. If a service fails to connect to middleware during startup, remind the user to check the middleware host.

```bash
# 1. Start the gateway
bash scripts/start-gateway.sh

# 2. Start the service you're working on
bash scripts/start-service.sh service-user

# 3. Start the frontend
bash scripts/start-frontend.sh app-b2c
```

All HTTP requests go through the gateway at `localhost:8999`. Swagger/Knife4j docs at `http://localhost:8999/doc.html`.

If a service fails with `Client not connected` or connection-refused errors, verify the middleware host is reachable:
```bash
curl -s -o /dev/null -w "%{http_code}" "http://${DOCKER_HOST_IP:-ecs4c16g}:8848/nacos/v1/console/health/readiness"
```

### Service Management Scripts

All scripts are in `scripts/` and auto-load `middleware-docker/.env` before starting. **Use scripts directly** — they set JVM flags, resolve env vars, redirect logs, **wait for health checks, and verify Nacos registration before reporting success**.

> **Important:** Scripts require `JAVA_HOME` pointing to JDK 21. If not set in your shell profile, export it before running scripts:
> ```bash
> export JAVA_HOME=D:/MyWorkStation/Java/jdk/jdk-21
> ```
> Or pass it inline: `JAVA_HOME=/path/to/jdk-21 bash scripts/start-service.sh service-user`

**Backend:**

```bash
# Start all 19 services (two phases: launch all → verify each with health + Nacos)
bash scripts/start-all.sh

# Start only the API gateway (waits for health + Nacos, exits non-zero on failure)
bash scripts/start-gateway.sh

# Start a single service (waits for health + Nacos, exits non-zero on failure)
bash scripts/start-service.sh service-user
bash scripts/start-service.sh service-order

# Stop all running super-market services (uses taskkill on Windows, kill on Unix)
bash scripts/stop-all.sh

# Check running services
jps -l | grep Application
```

**Frontend:**

```bash
# Start a single app
bash scripts/start-frontend.sh app-admin   # → localhost:5175
bash scripts/start-frontend.sh app-b2b     # → localhost:5174
bash scripts/start-frontend.sh app-b2c     # → localhost:5173

# Start all three
bash scripts/start-frontend.sh all

# Stop all frontend dev servers
bash scripts/stop-frontend.sh
```

JVM settings (set by scripts): microservices `-Xmx128m -Xms64m`, gateway `-Xmx256m -Xms128m`, all `-XX:+UseSerialGC` (G1 needs too much native memory at 128MB).

### Logs

All logs go to `logs/{service-name}/` at the project root. Each service has `application.log` (INFO+, 50MB × 30d, 2GB cap) and `error.log` (WARN+, 50MB × 90d, 1GB cap), configured via `src/main/resources/logback-spring.xml`.
