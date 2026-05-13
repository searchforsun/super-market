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
cd docker-compose && docker compose up -d

# Start a single service locally (after docker-compose is up)
mvn -pl super-market-services/service-user spring-boot:run
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
docker-compose/               # Dev middleware: MySQL, Redis, Nacos, RocketMQ, ES, MinIO, Sentinel, SkyWalking, Canal
docs/                         # PRD, solution design, implementation plans
```

### Key Patterns

- **Inter-service calls:** Services communicate via Dubbo 3.x Triple protocol. API interfaces live in `common-dubbo-api`. Service implementations register as Dubbo providers (`@EnableDubbo` + `application.yml` with `dubbo.protocol.name: tri`).
- **Gateway routing:** Spring Cloud Gateway routes HTTP requests by path prefix (e.g., `/api/user/**` → `user-service`). The gateway validates JWT tokens via `AuthGlobalFilter` and forwards `X-User-Id` and `X-User-Roles` headers to downstream services.
- **Service bootstrap:** Each service's `Application` class uses `scanBasePackages = {"com.supermarket.<service>", "com.supermarket.common"}` to pick up common module beans.
- **Unified response:** All REST endpoints return `R<T>` (code, message, data, timestamp) from `common-core`.
- **Configuration:** Uses Nacos for both discovery and config (config disabled in dev via `bootstrap.yml`). Dev profiles use `application-dev.yml` with hardcoded local connection strings.
- **Database:** Each service has its own database (vertical sharding). MyBatis-Plus with `assign_id` ID generation. Large tables (orders, products) use horizontal sharding with ShardingSphere-JDBC.
- **Service port ranges:** User domain 9301-9304, Product domain 9311-9314, Order domain 9321-9322, Payment 9331, Shop 9341, Marketing 9351-9352, Search 9361, File/Notify 9371-9372, Platform 9381.

### Developer Workflow

1. `cd docker-compose && docker compose up -d` — starts all middleware (MySQL, Redis, Nacos on :8848, RocketMQ, ES on :9200, MinIO on :9000)
2. Start the gateway: `mvn -pl super-market-gateway spring-boot:run`
3. Start the service(s) you're working on: `mvn -pl super-market-services/service-user spring-boot:run`
4. All HTTP requests go through the gateway at `localhost:8999`
5. Swagger/Knife4j docs available at `http://localhost:8999/doc.html`
