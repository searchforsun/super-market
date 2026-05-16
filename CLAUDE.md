# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules (skip tests)
mvn clean install -DskipTests

# Build all modules with tests
mvn clean test

# Run tests for a single module
mvn -pl super-market-services/service-user test

# Run a single test class
mvn -pl super-market-services/service-user test -Dtest=UserControllerTest

# Package a specific service
mvn -pl super-market-services/service-user package -DskipTests

# CheckStyle
mvn checkstyle:check

# Start development infrastructure (middleware)
cd middleware-docker && docker compose up -d

# Start a single service locally (after middleware-docker is up)
mvn -pl super-market-services/service-user spring-boot:run

# Frontend
cd frontend && pnpm install
cd frontend/app-b2c && pnpm dev     # port 5173
cd frontend/app-b2b && pnpm dev     # port 5174
cd frontend/app-admin && pnpm dev   # port 5175
```

## Architecture Overview

**Stack:** Java 21, Spring Boot 3.2.5, Dubbo 3.2.13 (Triple protocol), Spring Cloud Gateway 4.1

This is a JD.com-style e-commerce platform. The system is a Maven multi-module project with 18 microservices, an API gateway, and shared common modules. All services register with Nacos for service discovery, use Dubbo RPC for inter-service calls, and expose REST endpoints through the gateway.

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
scripts/                      # Service mgmt: start-all, start-gateway, start-service, start-frontend, stop-all, stop-frontend
docs/                         # PRD, solution design, implementation plans
```

### Key Patterns

- **Inter-service calls:** Services communicate via Dubbo 3.x Triple protocol. API interfaces live in `common-dubbo-api`. Service implementations register as Dubbo providers (`@EnableDubbo` + `application.yml` with `dubbo.protocol.name: tri`).
- **Gateway routing:** Spring Cloud Gateway routes HTTP requests by path prefix (e.g., `/api/user/**` → `user-service`). The gateway validates JWT tokens via `AuthGlobalFilter` and forwards `X-User-Id` and `X-User-Roles` headers to downstream services.
- **Service bootstrap:** Each service's `Application` class uses `scanBasePackages = {"com.supermarket.<service>", "com.supermarket.common"}` to pick up common module beans.
- **Unified response:** All REST endpoints return `R<T>` (code, message, data, timestamp) from `common-core`.
- **Configuration:** Uses Nacos for both discovery and config (config disabled in dev via `bootstrap.yml`). Dev profiles use `application-dev.yml` with `${ENV_VAR:default}` placeholders for host/port/credentials — see `middleware-docker/.env.example` for all supported variables. Scripts in `scripts/` auto-load `.env` before starting services.
- **Database:** Each service has its own database (vertical sharding). MyBatis-Plus with `assign_id` ID generation. Large tables (orders, products) use horizontal sharding with ShardingSphere-JDBC.
- **Service port ranges:** User domain 9301-9304, Product domain 9311-9314, Order domain 9321-9322, Payment 9331, Shop 9341, Marketing 9351-9352, Search 9361, File/Notify 9371-9372, Platform 9381.

### Environment Variables

All middleware host/port/credentials in microservice YAML configs use `${VAR:default}` placeholders:

| Variable | Default | Used by |
|---|---|---|
| `DOCKER_HOST_IP` | `localhost` | All middleware host connections |
| `NACOS_PORT` | `8848` | Nacos discovery, config, Dubbo registry |
| `NACOS_USERNAME` | `nacos` | Nacos auth |
| `NACOS_PASSWORD` | `nacos123` | Nacos auth |
| `MYSQL_PORT` | `3306` | MySQL datasource |
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL datasource |
| `REDIS_PORT` | `6379` | Redis |
| `REDIS_PASSWORD` | `redis123` | Redis |
| `ROCKETMQ_NAMESRV_PORT` | `9876` | RocketMQ name server |
| `ES01_PORT` | `9200` | Elasticsearch |
| `SEATA_PORT` | `8091` | Seata distributed transaction |

`middleware-docker/.env.example` documents all variables with standard ports. `middleware-docker/.env` is the actual config (gitignored, may use custom ports to avoid local conflicts). Scripts in `scripts/` auto-source `.env` and export all variables with defaults.

### Developer Workflow

1. `cd middleware-docker && docker compose up -d` — starts all middleware (MySQL, Redis, Nacos on :8848, RocketMQ, ES on :9200, MinIO on :9000)
2. `bash scripts/start-gateway.sh` — starts the gateway (env vars auto-loaded)
3. `bash scripts/start-service.sh service-user` — starts the service you're working on
4. `bash scripts/start-frontend.sh app-b2c` — starts the frontend app
5. All HTTP requests go through the gateway at `localhost:8999`
6. Swagger/Knife4j docs available at `http://localhost:8999/doc.html`

### Service Management Scripts

All scripts are in `scripts/` and auto-load `middleware-docker/.env` before starting.

**Backend:**

```bash
# Start all 19 services (gateway + 18 microservices)
bash scripts/start-all.sh

# Start only the API gateway
bash scripts/start-gateway.sh

# Start a single service
bash scripts/start-service.sh service-user
bash scripts/start-service.sh service-order

# Stop all running super-market services
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

All services use low-memory JVM settings by default:
- **Microservices (18):** `-Xmx128m -Xms64m -XX:+UseSerialGC`
- **Gateway:** `-Xmx256m -Xms128m -XX:+UseSerialGC`

> Note: `-XX:+UseSerialGC` is required with 128MB heap — the default G1 GC needs too much native memory for the JVM to start.

Override environment variables before running scripts:
```bash
export DOCKER_HOST_IP=192.168.1.100
export MYSQL_PORT=3307
bash scripts/start-service.sh service-user
```

### Log Directory Structure

All logs are written to `logs/` at the project root, with each service in its own subfolder:

```
logs/
├── gateway-service/
│   ├── application.log      # All INFO+ logs, rolling 30d / 2GB cap
│   └── error.log            # WARN+ only, rolling 90d / 1GB cap
├── user-service/
│   ├── application.log
│   └── error.log
├── order-service/
│   ├── application.log
│   └── error.log
├── ... (one folder per service)
│
└── (logback config: src/main/resources/logback-spring.xml per module)
```

Each `application.log`: 50MB per file, rotated daily, 30-day retention, 2GB total cap.
Each `error.log`: WARN+ level only, 50MB per file, 90-day retention, 1GB total cap.
