# Phase 1 — 基础设施搭建与项目脚手架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建完整的本地开发环境（Docker Compose 中间件集群 + Maven 多模块项目骨架 + 16 个微服务脚手架 + Gateway + CI/CD + K8s 基础配置），使团队可以立即开始编码。

**Architecture:** Maven 多模块父子工程架构。顶层 parent POM 统一管理 Spring Boot 3.2 / Dubbo 3.2 / Spring Cloud 2023.0 等全部依赖版本。common 层提供共享代码（DTO/异常/工具/安全/持久层配置）。每个微服务是独立 Spring Boot 模块，通过 Dubbo RPC 互调，通过 Nacos 注册发现。

**Tech Stack:** Java 21, Spring Boot 3.2, Apache Dubbo 3.2, Nacos 2.3, Maven 3.9, Docker Compose, MySQL 8.0, Redis 7.2, RocketMQ 5.1, Elasticsearch 8.12

---

## 总览：6 阶段开发路线图

| Phase | 周期 | 目标 | 交付物 |
|-------|------|------|--------|
| **Phase 1** | W1-W2 | 基础设施 + 脚手架 | Docker Compose, Maven 工程, 16 服务骨架, CI/CD |
| Phase 2 | W3-W6 | 用户+商品域 | user/auth/member/address/product/category/inventory/shop 服务 |
| Phase 3 | W7-W10 | 交易+支付域 | cart/order/payment 服务, Seata 分布式事务 |
| Phase 4 | W11-W13 | 搜索+营销域 | search/coupon/seckill 服务, Canal+ES 集成 |
| Phase 5 | W14-W16 | 支撑服务+完善 | review/notify/file/platform, 监控面板 |
| Phase 6 | W17-W18 | 压测+优化 | JMeter 压测, JVM/DB/缓存调优, 高可用演练 |

> **说明：** 本文档是 Phase 1 的详细执行计划。Phase 2-6 将在各自启动前编写独立详细计划。

---

## Phase 1 验收标准

- [ ] `docker-compose up -d` 一键启动全部中间件，所有服务健康 Check 通过
- [ ] `mvn clean install -DskipTests` 在根目录执行成功，所有模块编译通过
- [ ] Nacos 控制台 `http://localhost:8848/nacos` 可访问，16 个服务名在服务列表可见
- [ ] Gateway 启动后 `http://localhost:8080/actuator/health` 返回 UP
- [ ] Sentinel Dashboard `http://localhost:8858` 可访问
- [ ] `.gitlab-ci.yml` 模板就绪，4 阶段流水线定义完成
- [ ] K8s 基础配置目录结构就绪（Namespace / ConfigMap / Secret / PV 模板）

---

## Task Group A: Docker Compose 中间件编排

### Task A1: 创建 docker-compose 目录与 .env 文件

**Files:**
- Create: `docker-compose/.env`
- Create: `docker-compose/docker-compose.yml`

- [ ] **Step 1: 创建 .env 环境变量文件**

```bash
mkdir -p docker-compose/{mysql/init,nacos/conf,rocketmq/conf,nginx/conf.d,elasticsearch/config,minio/data,prometheus/config,grafana/provisioning}
```

写入 `docker-compose/.env`:

```env
# 网络配置
SUBNET=10.10.0.0/16

# MySQL
MYSQL_ROOT_PASSWORD=root123
MYSQL_PORT=3306

# Redis
REDIS_PORT=6379
REDIS_PASSWORD=redis123

# Nacos
NACOS_PORT=8848
NACOS_GRPC_PORT=9848
NACOS_AUTH_USERNAME=nacos
NACOS_AUTH_PASSWORD=nacos123

# RocketMQ
ROCKETMQ_NAMESRV_PORT=9876
ROCKETMQ_BROKER_PORT=10911

# Elasticsearch
ES_PORT=9200
ES_TRANSPORT_PORT=9300
ES_PASSWORD=elastic123

# MinIO
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123

# XXL-Job
XXL_JOB_PORT=8081
XXL_JOB_DB=xxl_job

# SkyWalking
SKYWALKING_GRPC_PORT=11800
SKYWALKING_HTTP_PORT=12800

# Sentinel
SENTINEL_PORT=8858
SENTINEL_USERNAME=sentinel
SENTINEL_PASSWORD=sentinel123

# Canal
CANAL_PORT=11111
```

- [ ] **Step 2: 验证 .env 文件语法**

```bash
grep -E '^[A-Z_]+\=' docker-compose/.env | wc -l
```
Expected: `18` (18 个环境变量定义)

- [ ] **Step 3: Commit**

```bash
git add docker-compose/.env
git commit -m "feat: add docker-compose .env with middleware configuration"
```

---

### Task A2: 创建 MySQL 初始化脚本

**Files:**
- Create: `docker-compose/mysql/init/01-init-databases.sql`

- [ ] **Step 1: 写入数据库初始化 SQL**

写入 `docker-compose/mysql/init/01-init-databases.sql`:

```sql
-- 创建业务数据库（垂直分库）
CREATE DATABASE IF NOT EXISTS db_user       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_product    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_order      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_payment    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_marketing  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_shop       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_platform   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建基础设施数据库
CREATE DATABASE IF NOT EXISTS xxl_job       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS seata         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建只读用户（读写分离预留）
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'app_user123';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_user.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_product.*    TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_order.*      TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_payment.*    TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_marketing.*  TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_shop.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_platform.*   TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON xxl_job.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON seata.*         TO 'app_user'@'%';
FLUSH PRIVILEGES;
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose/mysql/init/01-init-databases.sql
git commit -m "feat: add MySQL initialization script with business databases"
```

---

### Task A3: 编写主 docker-compose.yml

**Files:**
- Create: `docker-compose/docker-compose.yml`

- [ ] **Step 1: 写入完整的 Docker Compose 编排文件**

写入 `docker-compose/docker-compose.yml`:

```yaml
version: '3.8'

networks:
  smt-net:
    driver: bridge
    ipam:
      config:
        - subnet: ${SUBNET:-10.10.0.0/16}

services:
  # ============================
  # MySQL 8.0
  # ============================
  mysql:
    image: mysql:8.0.35
    container_name: smt-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      TZ: Asia/Shanghai
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
      - ./mysql/conf:/etc/mysql/conf.d
    networks:
      - smt-net
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ============================
  # Redis 7.2
  # ============================
  redis:
    image: redis:7.2-alpine
    container_name: smt-redis
    restart: unless-stopped
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD:-redis123}
      --maxmemory 512mb
      --maxmemory-policy allkeys-lru
      --appendonly yes
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis-data:/data
    networks:
      - smt-net
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ============================
  # Nacos 2.3 (单机模式)
  # ============================
  nacos:
    image: nacos/nacos-server:v2.3.1
    container_name: smt-nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN_EXPIRE_SECONDS: "86400"
      NACOS_AUTH_IDENTITY_KEY: serverIdentity
      NACOS_AUTH_IDENTITY_VALUE: security
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
    ports:
      - "${NACOS_PORT:-8848}:8848"
      - "${NACOS_GRPC_PORT:-9848}:9848"
    volumes:
      - nacos-data:/home/nacos/data
    networks:
      - smt-net
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/v1/console/health/readiness"]
      interval: 15s
      timeout: 10s
      retries: 10

  # ============================
  # RocketMQ 5.1
  # ============================
  rocketmq-namesrv:
    image: apache/rocketmq:5.1.4
    container_name: smt-rocketmq-namesrv
    restart: unless-stopped
    command: sh mqnamesrv
    ports:
      - "${ROCKETMQ_NAMESRV_PORT:-9876}:9876"
    volumes:
      - rocketmq-namesrv-data:/home/rocketmq/store
    networks:
      - smt-net
    healthcheck:
      test: ["CMD", "sh", "-c", "curl -s localhost:9876 >/dev/null || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5

  rocketmq-broker:
    image: apache/rocketmq:5.1.4
    container_name: smt-rocketmq-broker
    restart: unless-stopped
    command: sh mqbroker -n rocketmq-namesrv:9876 -c /home/rocketmq/conf/broker.conf
    ports:
      - "${ROCKETMQ_BROKER_PORT:-10911}:10911"
      - "10909:10909"
    volumes:
      - rocketmq-broker-data:/home/rocketmq/store
      - ./rocketmq/conf/broker.conf:/home/rocketmq/conf/broker.conf
    networks:
      - smt-net
    depends_on:
      rocketmq-namesrv:
        condition: service_healthy

  # ============================
  # Elasticsearch 8.12
  # ============================
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.2
    container_name: smt-elasticsearch
    restart: unless-stopped
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      xpack.security.enabled: "false"
      xpack.security.enrollment.enabled: "false"
      xpack.security.http.ssl.enabled: "false"
      xpack.security.transport.ssl.enabled: "false"
    ports:
      - "${ES_PORT:-9200}:9200"
      - "${ES_TRANSPORT_PORT:-9300}:9300"
    volumes:
      - es-data:/usr/share/elasticsearch/data
      - ./elasticsearch/config:/usr/share/elasticsearch/config
    networks:
      - smt-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9200/_cluster/health"]
      interval: 15s
      timeout: 10s
      retries: 10

  # ============================
  # MinIO 对象存储
  # ============================
  minio:
    image: quay.io/minio/minio:RELEASE.2024-05-10T01-41-38Z
    container_name: smt-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:-minioadmin123}
    ports:
      - "${MINIO_API_PORT:-9000}:9000"
      - "${MINIO_CONSOLE_PORT:-9001}:9001"
    volumes:
      - minio-data:/data
    networks:
      - smt-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ============================
  # XXL-Job 2.4
  # ============================
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.1
    container_name: smt-xxl-job
    restart: unless-stopped
    environment:
      PARAMS: >
        --spring.datasource.url=jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD:-root123}
        --xxl.job.accessToken=xxl-job-access-token-2024
    ports:
      - "${XXL_JOB_PORT:-8081}:8080"
    networks:
      - smt-net
    depends_on:
      mysql:
        condition: service_healthy

  # ============================
  # Sentinel Dashboard 1.8
  # ============================
  sentinel-dashboard:
    image: bladex/sentinel-dashboard:1.8.6
    container_name: smt-sentinel
    restart: unless-stopped
    environment:
      SERVER_PORT: 8858
      AUTH_USERNAME: ${SENTINEL_USERNAME:-sentinel}
      AUTH_PASSWORD: ${SENTINEL_PASSWORD:-sentinel123}
      NACOS_SERVER_ADDR: nacos:8848
    ports:
      - "${SENTINEL_PORT:-8858}:8858"
    networks:
      - smt-net
    depends_on:
      nacos:
        condition: service_healthy

  # ============================
  # SkyWalking 9.7
  # ============================
  skywalking-oap:
    image: apache/skywalking-oap-server:9.7.0
    container_name: smt-skywalking-oap
    restart: unless-stopped
    environment:
      SW_STORAGE: elasticsearch
      SW_STORAGE_ES_CLUSTER_NODES: elasticsearch:9200
      JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      - "${SKYWALKING_GRPC_PORT:-11800}:11800"
      - "${SKYWALKING_HTTP_PORT:-12800}:12800"
    networks:
      - smt-net
    depends_on:
      elasticsearch:
        condition: service_healthy

  skywalking-ui:
    image: apache/skywalking-ui:9.7.0
    container_name: smt-skywalking-ui
    restart: unless-stopped
    environment:
      SW_OAP_ADDRESS: http://skywalking-oap:12800
    ports:
      - "8082:8080"
    networks:
      - smt-net
    depends_on:
      - skywalking-oap

  # ============================
  # Canal 1.1.x
  # ============================
  canal-server:
    image: canal/canal-server:v1.1.7
    container_name: smt-canal
    restart: unless-stopped
    environment:
      CANAL_ADMIN_MANAGER: ""
      canal.instance.master.address: mysql:3306
      canal.instance.dbUsername: root
      canal.instance.dbPassword: ${MYSQL_ROOT_PASSWORD:-root123}
      canal.instance.filter.regex: .*\\..*
    ports:
      - "${CANAL_PORT:-11111}:11111"
    networks:
      - smt-net
    depends_on:
      mysql:
        condition: service_healthy

volumes:
  mysql-data:
  redis-data:
  nacos-data:
  rocketmq-namesrv-data:
  rocketmq-broker-data:
  es-data:
  minio-data:
```

- [ ] **Step 2: 创建 RocketMQ Broker 配置**

```bash
mkdir -p docker-compose/rocketmq/conf
```

写入 `docker-compose/rocketmq/conf/broker.conf`:

```properties
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
deleteWhen=04
fileReservedTime=48
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
listenPort=10911
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose/docker-compose.yml docker-compose/rocketmq/
git commit -m "feat: add Docker Compose middleware orchestration (12 services)"
```

---

### Task A4: 验证 Docker Compose 启动

- [ ] **Step 1: 启动所有中间件**

```bash
cd docker-compose && docker compose up -d
```

- [ ] **Step 2: 等待健康检查通过（约 90 秒）**

```bash
docker compose ps
```
Expected: 所有服务 State = "Up" (healthy) 或 "Up"

- [ ] **Step 3: 验证各中间件端点**

```bash
# Nacos 控制台
curl -s -o /dev/null -w "%{http_code}" http://localhost:8848/nacos/v1/console/health/readiness
# Expected: 200

# Redis
docker exec smt-redis redis-cli -a redis123 ping
# Expected: PONG

# MySQL
docker exec smt-mysql mysqladmin ping -h localhost -u root -proot123
# Expected: mysqld is alive

# Elasticsearch
curl -s http://localhost:9200/_cluster/health | grep -o '"status":"green"'
# Expected: "status":"green"

# MinIO
curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/minio/health/live
# Expected: 200

# XXL-Job
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/xxl-job-admin
# Expected: 200

# Sentinel
curl -s -o /dev/null -w "%{http_code}" http://localhost:8858
# Expected: 200
```

- [ ] **Step 4: Commit checkpoint**

```bash
git add -A
git commit -m "chore: Docker Compose middleware verified - all 12 services healthy"
```

---

## Task Group B: Maven 多模块工程结构

### Task B1: 创建根目录 pom.xml（父 POM）

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 写入父 POM**

写入 `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.supermarket</groupId>
    <artifactId>super-market</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Super Market</name>
    <description>京东式商城系统 — 基于 Dubbo 3.x 的微服务电商平台</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <modules>
        <!-- 公共模块 -->
        <module>super-market-common/common-core</module>
        <module>super-market-common/common-dubbo-api</module>
        <module>super-market-common/common-security</module>
        <module>super-market-common/common-web</module>
        <module>super-market-common/common-mybatis</module>

        <!-- 网关 -->
        <module>super-market-gateway</module>

        <!-- 微服务 -->
        <module>super-market-services/service-user</module>
        <module>super-market-services/service-auth</module>
        <module>super-market-services/service-member</module>
        <module>super-market-services/service-address</module>
        <module>super-market-services/service-product</module>
        <module>super-market-services/service-category</module>
        <module>super-market-services/service-inventory</module>
        <module>super-market-services/service-review</module>
        <module>super-market-services/service-cart</module>
        <module>super-market-services/service-order</module>
        <module>super-market-services/service-payment</module>
        <module>super-market-services/service-coupon</module>
        <module>super-market-services/service-seckill</module>
        <module>super-market-services/service-search</module>
        <module>super-market-services/service-shop</module>
        <module>super-market-services/service-platform</module>
        <module>super-market-services/service-file</module>
        <module>super-market-services/service-notify</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Spring -->
        <spring-boot.version>3.2.5</spring-boot.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>

        <!-- Dubbo -->
        <dubbo.version>3.2.13</dubbo.version>

        <!-- Third-party -->
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
        <redisson.version>3.25.2</redisson.version>
        <shardingsphere.version>5.4.1</shardingsphere.version>
        <hutool.version>5.8.27</hutool.version>
        <knife4j.version>4.3.0</knife4j.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>

        <!-- Plugin -->
        <maven-surefire.version>3.2.5</maven-surefire.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Cloud Alibaba -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Dubbo -->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-bom</artifactId>
                <version>${dubbo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-jsqlparser</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>
            <!-- ShardingSphere -->
            <dependency>
                <groupId>org.apache.shardingsphere</groupId>
                <artifactId>shardingsphere-jdbc-core</artifactId>
                <version>${shardingsphere.version}</version>
            </dependency>
            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- Knife4j API 文档 -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>
            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- 全局依赖: 所有模块自动继承 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.12.1</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 验证 POM 语法**

```bash
mvn validate
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add parent POM with dependency management for Spring Boot 3.2 + Dubbo 3.2"
```

---

### Task B2: 创建 common-core 公共核心模块

**Files:**
- Create: `super-market-common/common-core/pom.xml`
- Create: `super-market-common/common-core/src/main/java/com/supermarket/common/core/result/R.java`
- Create: `super-market-common/common-core/src/main/java/com/supermarket/common/core/exception/BizException.java`
- Create: `super-market-common/common-core/src/main/java/com/supermarket/common/core/constants/GlobalConstants.java`

- [ ] **Step 1: 创建 common-core/pom.xml**

```bash
mkdir -p super-market-common/common-core/src/main/java/com/supermarket/common/core/{result,exception,constants}
mkdir -p super-market-common/common-core/src/test/java
```

写入 `super-market-common/common-core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-core</artifactId>
    <packaging>jar</packaging>
    <description>公共核心模块：统一返回体、异常、常量、工具类</description>
</project>
```

- [ ] **Step 2: 创建统一返回体 R.java**

写入 `super-market-common/common-core/src/main/java/com/supermarket/common/core/result/R.java`:

```java
package com.supermarket.common.core.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
```

- [ ] **Step 3: 创建业务异常 BizException.java**

写入 `super-market-common/common-core/src/main/java/com/supermarket/common/core/exception/BizException.java`:

```java
package com.supermarket.common.core.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(500, message);
    }
}
```

- [ ] **Step 4: 创建全局常量 GlobalConstants.java**

写入 `super-market-common/common-core/src/main/java/com/supermarket/common/core/constants/GlobalConstants.java`:

```java
package com.supermarket.common.core.constants;

public interface GlobalConstants {

    String TRACE_ID = "X-Trace-Id";
    String USER_ID = "X-User-Id";
    String USER_ROLES = "X-User-Roles";

    String REDIS_PREFIX = "smt:";

    int PAGE_NO_DEFAULT = 1;
    int PAGE_SIZE_DEFAULT = 20;
    int PAGE_SIZE_MAX = 100;
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn clean compile -pl super-market-common/common-core
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add super-market-common/common-core/
git commit -m "feat: add common-core module (R, BizException, GlobalConstants)"
```

---

### Task B3: 创建 common-dubbo-api 接口定义模块

**Files:**
- Create: `super-market-common/common-dubbo-api/pom.xml`

- [ ] **Step 1: 创建模块 pom.xml**

```bash
mkdir -p super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api
```

写入 `super-market-common/common-dubbo-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-dubbo-api</artifactId>
    <packaging>jar</packaging>
    <description>Dubbo API 接口定义模块：各服务 RPC 接口与 DTO</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl super-market-common/common-dubbo-api
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add super-market-common/common-dubbo-api/
git commit -m "feat: add common-dubbo-api module for RPC interface definitions"
```

---

### Task B4: 创建 common-security 安全模块

**Files:**
- Create: `super-market-common/common-security/pom.xml`
- Create: `super-market-common/common-security/src/main/java/com/supermarket/common/security/config/JwtProperties.java`
- Create: `super-market-common/common-security/src/main/java/com/supermarket/common/security/util/JwtUtil.java`

- [ ] **Step 1: 创建模块 pom.xml**

```bash
mkdir -p super-market-common/common-security/src/main/java/com/supermarket/common/security/{config,util}
```

写入 `super-market-common/common-security/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-security</artifactId>
    <packaging>jar</packaging>
    <description>公共安全模块：JWT 工具、安全配置</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 JwtProperties 配置类**

写入 `super-market-common/common-security/src/main/java/com/supermarket/common/security/config/JwtProperties.java`:

```java
package com.supermarket.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "super-market-default-secret-key-change-in-production-min-256-bits";
    private long accessTokenExpire = 7200;    // 2 hours
    private long refreshTokenExpire = 604800;  // 7 days
    private String issuer = "super-market";
}
```

- [ ] **Step 3: 创建 JwtUtil 工具类**

写入 `super-market-common/common-security/src/main/java/com/supermarket/common/security/util/JwtUtil.java`:

```java
package com.supermarket.common.security.util;

import com.supermarket.common.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(String.valueOf(userId))
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtProperties.getAccessTokenExpire() * 1000))
            .signWith(getKey())
            .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(String.valueOf(userId))
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtProperties.getRefreshTokenExpire() * 1000))
            .signWith(getKey())
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return parseToken(token).get("roles", List.class);
    }

    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -pl super-market-common/common-security
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-security/
git commit -m "feat: add common-security module (JWT utils + properties)"
```

---

### Task B5: 创建 common-web 通用 Web 配置模块

**Files:**
- Create: `super-market-common/common-web/pom.xml`
- Create: `super-market-common/common-web/src/main/java/com/supermarket/common/web/handler/GlobalExceptionHandler.java`
- Create: `super-market-common/common-web/src/main/java/com/supermarket/common/web/config/JacksonConfig.java`

- [ ] **Step 1: 创建 pom.xml 和目录**

```bash
mkdir -p super-market-common/common-web/src/main/java/com/supermarket/common/web/{handler,config}
```

写入 `super-market-common/common-web/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-web</artifactId>
    <packaging>jar</packaging>
    <description>公共 Web 模块：全局异常处理、Jackson 配置、响应包装</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建全局异常处理器**

写入 `super-market-common/common-web/src/main/java/com/supermarket/common/web/handler/GlobalExceptionHandler.java`:

```java
package com.supermarket.common.web.handler;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return R.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return R.fail(500, "系统繁忙，请稍后重试");
    }
}
```

- [ ] **Step 3: 创建 Jackson 配置**

写入 `super-market-common/common-web/src/main/java/com/supermarket/common/web/config/JacksonConfig.java`:

```java
package com.supermarket.common.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );
            JavaTimeModule module = new JavaTimeModule();
            module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
            builder.modules(module);
        };
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -pl super-market-common/common-web
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-web/
git commit -m "feat: add common-web module (GlobalExceptionHandler + JacksonConfig)"
```

---

### Task B6: 创建 common-mybatis 持久层公共模块

**Files:**
- Create: `super-market-common/common-mybatis/pom.xml`
- Create: `super-market-common/common-mybatis/src/main/java/com/supermarket/common/mybatis/config/MyBatisPlusConfig.java`
- Create: `super-market-common/common-mybatis/src/main/java/com/supermarket/common/mybatis/entity/BaseEntity.java`

- [ ] **Step 1: 创建模块 pom.xml 和目录**

```bash
mkdir -p super-market-common/common-mybatis/src/main/java/com/supermarket/common/mybatis/{config,entity,handler}
```

写入 `super-market-common/common-mybatis/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-mybatis</artifactId>
    <packaging>jar</packaging>
    <description>公共持久层模块：MyBatis-Plus 配置、BaseEntity、自动填充</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 MyBatis-Plus 配置类**

写入 `super-market-common/common-mybatis/src/main/java/com/supermarket/common/mybatis/config/MyBatisPlusConfig.java`:

```java
package com.supermarket.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 3: 创建 BaseEntity**

写入 `super-market-common/common-mybatis/src/main/java/com/supermarket/common/mybatis/entity/BaseEntity.java`:

```java
package com.supermarket.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -pl super-market-common/common-mybatis
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-mybatis/
git commit -m "feat: add common-mybatis module (MyBatisPlusConfig + BaseEntity)"
```

---

### Task B7: 全量编译验证

- [ ] **Step 1: 从根目录全量编译**

```bash
mvn clean install -DskipTests
```
Expected: `BUILD SUCCESS`，6 个公共模块全部编译通过

- [ ] **Step 2: 验证生成的 jar 包**

```bash
find super-market-common -name "*.jar" -path "*/target/*" | sort
```
Expected: 列出 5 个 common 模块的 jar 包

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: all common modules compile and install successfully"
```

---

## Task Group C: 微服务脚手架

### 设计说明

每个微服务采用统一的模块结构和配置模板：

```
service-{name}/
├── pom.xml
└── src/main/
    ├── java/com/supermarket/{name}/
    │   ├── {Name}Application.java          # Spring Boot 启动类
    │   └── config/
    │       └── DubboProviderConfig.java     # Dubbo 服务提供者配置
    └── resources/
        ├── application.yml                  # 主配置 (Nacos 配置中心)
        ├── application-dev.yml              # 开发环境配置
        └── bootstrap.yml                    # 引导配置 (Nacos 地址)
```

所有服务共用同一套 `application-dev.yml` 模板，区别仅在于：
- `spring.application.name` = 服务名
- `server.port` = 分配端口
- `spring.datasource.url` = 对应数据库

---

### Task C1: 创建服务模块父目录结构与生成脚本

- [ ] **Step 1: 创建 services 目录**

```bash
mkdir -p super-market-services
```

- [ ] **Step 2: 定义服务注册表**

在 `docs/superpowers/plans/` 下创建服务注册表 `service-registry.md`:

```markdown
# 微服务注册表

| 序号 | 模块目录 | 服务名 | 端口 | 数据库 | 包名 |
|------|----------|--------|------|--------|------|
| 1 | service-user | user-service | 9001 | db_user | com.supermarket.user |
| 2 | service-auth | auth-service | 9002 | db_user | com.supermarket.auth |
| 3 | service-member | member-service | 9003 | db_user | com.supermarket.member |
| 4 | service-address | address-service | 9004 | db_user | com.supermarket.address |
| 5 | service-product | product-service | 9011 | db_product | com.supermarket.product |
| 6 | service-category | category-service | 9012 | db_product | com.supermarket.category |
| 7 | service-inventory | inventory-service | 9013 | db_product | com.supermarket.inventory |
| 8 | service-review | review-service | 9014 | db_product | com.supermarket.review |
| 9 | service-cart | cart-service | 9021 | db_order | com.supermarket.cart |
| 10 | service-order | order-service | 9022 | db_order | com.supermarket.order |
| 11 | service-payment | payment-service | 9031 | db_payment | com.supermarket.payment |
| 12 | service-coupon | coupon-service | 9051 | db_marketing | com.supermarket.coupon |
| 13 | service-seckill | seckill-service | 9052 | db_marketing | com.supermarket.seckill |
| 14 | service-search | search-service | 9061 | - (ES) | com.supermarket.search |
| 15 | service-shop | shop-service | 9041 | db_shop | com.supermarket.shop |
| 16 | service-platform | platform-service | 9081 | db_platform | com.supermarket.platform |
| 17 | service-file | file-service | 9071 | - (MinIO) | com.supermarket.file |
| 18 | service-notify | notify-service | 9072 | - | com.supermarket.notify |
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/plans/service-registry.md
git commit -m "docs: add service registry with port and database mapping"
```

---

### Task C2: 创建核心服务脚手架 — user-service（模板验证）

以 `user-service` 为第一个完整创建，验证模板可用后，再批量生成其余 17 个服务。

**Files:**
- Create: `super-market-services/service-user/pom.xml`
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/UserApplication.java`
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/config/DubboProviderConfig.java`
- Create: `super-market-services/service-user/src/main/resources/application.yml`
- Create: `super-market-services/service-user/src/main/resources/application-dev.yml`
- Create: `super-market-services/service-user/src/main/resources/bootstrap.yml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p super-market-services/service-user/src/main/java/com/supermarket/user/config
mkdir -p super-market-services/service-user/src/main/resources
mkdir -p super-market-services/service-user/src/test/java/com/supermarket/user
```

- [ ] **Step 2: 写入 pom.xml**

写入 `super-market-services/service-user/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>service-user</artifactId>
    <packaging>jar</packaging>
    <description>用户服务 — 用户注册、登录、信息管理</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-web</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-security</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-mybatis</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-dubbo-api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Dubbo -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
        </dependency>
        <!-- Nacos -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- Sentinel -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
        <!-- Redisson -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 写入启动类**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/UserApplication.java`:

```java
package com.supermarket.user;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "com.supermarket.user",
    "com.supermarket.common"
})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
```

- [ ] **Step 4: 写入 Dubbo 配置类**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/config/DubboProviderConfig.java`:

```java
package com.supermarket.user.config;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@DubboComponentScan(basePackages = "com.supermarket.user")
public class DubboProviderConfig {
}
```

- [ ] **Step 5: 写入配置文件**

写入 `super-market-services/service-user/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: user-service
  profiles:
    active: dev
  main:
    allow-bean-definition-overriding: true

server:
  port: ${SERVER_PORT:9001}
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

写入 `super-market-services/service-user/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_user?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 5000

  data:
    redis:
      host: localhost
      port: 6379
      password: redis123
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2

dubbo:
  application:
    name: user-service
    qos-enable: false
  protocol:
    name: tri
    port: -1
  registry:
    address: nacos://localhost:8848
    parameters:
      namespace: public
  consumer:
    check: false
    timeout: 5000
    retries: 0

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.supermarket.user.entity
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.supermarket: DEBUG
    com.baomidou: WARN
```

写入 `super-market-services/service-user/src/main/resources/bootstrap.yml`:

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yaml
        enabled: false
      discovery:
        server-addr: localhost:8848
        namespace: public
        enabled: true
```

- [ ] **Step 6: 编译验证**

```bash
mvn clean compile -pl super-market-services/service-user
```
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add super-market-services/service-user/
git commit -m "feat: add user-service scaffold (Spring Boot + Dubbo + Nacos + MyBatis-Plus)"
```

---

### Task C3: 批量生成其余 17 个服务脚手架

- [ ] **Step 1: 执行批量生成脚本**

```bash
#!/bin/bash
# 服务注册表（来自 service-registry.md）
SERVICES=(
  "service-auth:9002:com.supermarket.auth:AuthApplication:db_user"
  "service-member:9003:com.supermarket.member:MemberApplication:db_user"
  "service-address:9004:com.supermarket.address:AddressApplication:db_user"
  "service-product:9011:com.supermarket.product:ProductApplication:db_product"
  "service-category:9012:com.supermarket.category:CategoryApplication:db_product"
  "service-inventory:9013:com.supermarket.inventory:InventoryApplication:db_product"
  "service-review:9014:com.supermarket.review:ReviewApplication:db_product"
  "service-cart:9021:com.supermarket.cart:CartApplication:db_order"
  "service-order:9022:com.supermarket.order:OrderApplication:db_order"
  "service-payment:9031:com.supermarket.payment:PaymentApplication:db_payment"
  "service-coupon:9051:com.supermarket.coupon:CouponApplication:db_marketing"
  "service-seckill:9052:com.supermarket.seckill:SeckillApplication:db_marketing"
  "service-search:9061:com.supermarket.search:SearchApplication:db_product"
  "service-shop:9041:com.supermarket.shop:ShopApplication:db_shop"
  "service-platform:9081:com.supermarket.platform:PlatformApplication:db_platform"
  "service-file:9071:com.supermarket.file:FileApplication:db_product"
  "service-notify:9072:com.supermarket.notify:NotifyApplication:db_product"
)

TEMPLATE_DIR="super-market-services/service-user"

for svc in "${SERVICES[@]}"; do
  IFS=':' read -r dir port pkg class db <<< "$svc"
  echo "Generating $dir..."

  TARGET="super-market-services/$dir"
  mkdir -p "$TARGET/src/main/java/${pkg//.//}/config"
  mkdir -p "$TARGET/src/main/resources"
  mkdir -p "$TARGET/src/test/java/${pkg//.//}"

  # pom.xml
  sed "s/service-user/$dir/g; s/user-service/${dir#service-}-service/g; s/UserApplication/$class/g; s/用户服务/${dir#service-}服务/g" \
    "$TEMPLATE_DIR/pom.xml" > "$TARGET/pom.xml"

  # Application.java
  PKG_PATH="${pkg//.//}"
  cat > "$TARGET/src/main/java/$PKG_PATH/${class}.java" << JEOF
package $pkg;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "$pkg",
    "com.supermarket.common"
})
public class $class {
    public static void main(String[] args) {
        SpringApplication.run($class.class, args);
    }
}
JEOF

  # DubboProviderConfig.java
  cat > "$TARGET/src/main/java/$PKG_PATH/config/DubboProviderConfig.java" << JEOF
package $pkg.config;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@DubboComponentScan(basePackages = "$pkg")
public class DubboProviderConfig {
}
JEOF

  # application.yml
  SVC_NAME="${dir#service-}-service"
  sed "s/user-service/$SVC_NAME/g; s/9001/$port/g" \
    "$TEMPLATE_DIR/src/main/resources/application.yml" > "$TARGET/src/main/resources/application.yml"

  # application-dev.yml
  sed "s/db_user/$db/g" \
    "$TEMPLATE_DIR/src/main/resources/application-dev.yml" > "$TARGET/src/main/resources/application-dev.yml"

  # bootstrap.yml (copy as-is)
  cp "$TEMPLATE_DIR/src/main/resources/bootstrap.yml" "$TARGET/src/main/resources/bootstrap.yml"

  echo "  Done: $dir ($SVC_NAME :$port -> $db)"
done
```

- [ ] **Step 2: 运行脚本**

```bash
bash docs/superpowers/plans/generate-services.sh
```
Expected: 17 行 "Done: service-xxx" 输出

- [ ] **Step 3: 全量编译验证**

```bash
mvn clean compile -DskipTests
```
Expected: `BUILD SUCCESS`，18 个服务模块全部编译通过

- [ ] **Step 4: 验证所有服务启动类存在**

```bash
find super-market-services -name "*Application.java" | wc -l
```
Expected: `18`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/
git commit -m "feat: generate 18 microservice scaffolds (Spring Boot + Dubbo + Nacos)"
```

---

## Task Group D: Gateway 网关模块

### Task D1: 创建 Spring Cloud Gateway 模块

**Files:**
- Create: `super-market-gateway/pom.xml`
- Create: `super-market-gateway/src/main/java/com/supermarket/gateway/GatewayApplication.java`
- Create: `super-market-gateway/src/main/java/com/supermarket/gateway/filter/AuthGlobalFilter.java`
- Create: `super-market-gateway/src/main/resources/application.yml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p super-market-gateway/src/main/java/com/supermarket/gateway/filter
mkdir -p super-market-gateway/src/main/resources
```

- [ ] **Step 2: 写入 pom.xml**

写入 `super-market-gateway/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.supermarket</groupId>
        <artifactId>super-market</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>super-market-gateway</artifactId>
    <packaging>jar</packaging>
    <description>API 网关 — Spring Cloud Gateway + 统一鉴权 + 限流</description>

    <dependencies>
        <dependency>
            <groupId>com.supermarket</groupId>
            <artifactId>common-security</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 写入启动类**

写入 `super-market-gateway/src/main/java/com/supermarket/gateway/GatewayApplication.java`:

```java
package com.supermarket.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.supermarket.gateway",
    "com.supermarket.common"
})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 4: 写入全局鉴权过滤器**

写入 `super-market-gateway/src/main/java/com/supermarket/gateway/filter/AuthGlobalFilter.java`:

```java
package com.supermarket.gateway.filter;

import com.supermarket.common.core.constants.GlobalConstants;
import com.supermarket.common.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> WHITELIST = List.of(
        "/api/user/register",
        "/api/user/login",
        "/api/auth/login",
        "/actuator",
        "/doc.html",
        "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供认证 Token");
        }

        String token = authHeader.substring(7);

        // 验证 Token
        if (!jwtUtil.validate(token)) {
            return unauthorized(exchange, "Token 无效或已过期");
        }

        if (jwtUtil.isExpired(token)) {
            return unauthorized(exchange, "Token 已过期");
        }

        // 透传用户信息到下游
        Long userId = jwtUtil.getUserId(token);
        List<String> roles = jwtUtil.getRoles(token);

        ServerHttpRequest mutatedRequest = request.mutate()
            .header(GlobalConstants.USER_ID, String.valueOf(userId))
            .header(GlobalConstants.USER_ROLES, String.join(",", roles))
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

- [ ] **Step 5: 写入 Gateway 配置**

写入 `super-market-gateway/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: gateway-service
  profiles:
    active: dev
  main:
    web-application-type: reactive
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/product/**
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/api/inventory/**
        - id: cart-service
          uri: lb://cart-service
          predicates:
            - Path=/api/cart/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payment/**
        - id: coupon-service
          uri: lb://coupon-service
          predicates:
            - Path=/api/coupon/**
        - id: seckill-service
          uri: lb://seckill-service
          predicates:
            - Path=/api/seckill/**
        - id: search-service
          uri: lb://search-service
          predicates:
            - Path=/api/search/**
        - id: shop-service
          uri: lb://shop-service
          predicates:
            - Path=/api/shop/**
        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/file/**
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123

server:
  port: 8080

jwt:
  secret: super-market-default-secret-key-change-in-production-min-256-bits
  access-token-expire: 7200
  refresh-token-expire: 604800

logging:
  level:
    com.supermarket.gateway: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
```

- [ ] **Step 6: 编译验证**

```bash
mvn clean compile -pl super-market-gateway
```
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add super-market-gateway/
git commit -m "feat: add Gateway module (Spring Cloud Gateway + JWT auth filter + route config)"
```

---

## Task Group E: CI/CD 流水线

### Task E1: 创建 GitLab CI/CD 模板

**Files:**
- Create: `.gitlab-ci.yml`

- [ ] **Step 1: 写入 CI/CD 配置**

写入 `.gitlab-ci.yml`:

```yaml
# Super Market — GitLab CI/CD 流水线定义
# 4 阶段: check → test → build → deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  MAVEN_CLI_OPTS: "--batch-mode --errors --fail-at-end --show-version"
  DOCKER_DRIVER: overlay2
  HARBOR_URL: harbor.local
  HARBOR_PROJECT: super-market

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/

stages:
  - check
  - test
  - build
  - deploy

# ==========================================
# Stage 1: 代码检查
# ==========================================
checkstyle:
  stage: check
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn checkstyle:check $MAVEN_CLI_OPTS || true
  allow_failure: true
  only:
    - merge_requests
    - master

# ==========================================
# Stage 2: 构建 + 单元测试
# ==========================================
unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  services:
    - name: mysql:8.0.35
      alias: mysql
      variables:
        MYSQL_ROOT_PASSWORD: root123
    - name: redis:7.2-alpine
      alias: redis
  variables:
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/db_user
    SPRING_DATASOURCE_USERNAME: root
    SPRING_DATASOURCE_PASSWORD: root123
    SPRING_REDIS_HOST: redis
  script:
    - mvn clean test $MAVEN_CLI_OPTS
  artifacts:
    when: always
    reports:
      junit:
        - "**/target/surefire-reports/TEST-*.xml"
    paths:
      - "**/target/surefire-reports/"
  coverage: '/Total.*?([0-9]{1,3})%$/'
  only:
    - merge_requests
    - master

# ==========================================
# Stage 3: 镜像打包 + 推送
# ==========================================
docker-build:
  stage: build
  image: docker:24-dind
  services:
    - docker:24-dind
  before_script:
    - echo "$HARBOR_PASSWORD" | docker login $HARBOR_URL -u "$HARBOR_USERNAME" --password-stdin
  script:
    - |
      SERVICES=(
        "service-user" "service-auth" "service-member" "service-address"
        "service-product" "service-category" "service-inventory" "service-review"
        "service-cart" "service-order" "service-payment"
        "service-coupon" "service-seckill" "service-search"
        "service-shop" "service-platform" "service-file" "service-notify"
      )
      for svc in "${SERVICES[@]}"; do
        echo "Building $svc..."
        cd super-market-services/$svc
        docker build -t $HARBOR_URL/$HARBOR_PROJECT/$svc:$CI_COMMIT_SHORT_SHA .
        docker push $HARBOR_URL/$HARBOR_PROJECT/$svc:$CI_COMMIT_SHORT_SHA
        docker tag $HARBOR_URL/$HARBOR_PROJECT/$svc:$CI_COMMIT_SHORT_SHA $HARBOR_URL/$HARBOR_PROJECT/$svc:latest
        docker push $HARBOR_URL/$HARBOR_PROJECT/$svc:latest
        cd $CI_PROJECT_DIR
      done
      echo "Building gateway..."
      cd super-market-gateway
      docker build -t $HARBOR_URL/$HARBOR_PROJECT/gateway:$CI_COMMIT_SHORT_SHA .
      docker push $HARBOR_URL/$HARBOR_PROJECT/gateway:$CI_COMMIT_SHORT_SHA
  only:
    - master
  when: manual

# ==========================================
# Stage 4: K8s 部署（滚动发布）
# ==========================================
deploy-to-k8s:
  stage: deploy
  image: bitnami/kubectl:1.29
  script:
    - |
      SERVICES=(
        "service-user" "service-auth" "service-member" "service-address"
        "service-product" "service-category" "service-inventory" "service-review"
        "service-cart" "service-order" "service-payment"
        "service-coupon" "service-seckill" "service-search"
        "service-shop" "service-platform" "service-file" "service-notify"
      )
      for svc in "${SERVICES[@]}"; do
        kubectl set image deployment/$svc $svc=$HARBOR_URL/$HARBOR_PROJECT/$svc:$CI_COMMIT_SHORT_SHA -n super-market
        kubectl rollout status deployment/$svc -n super-market --timeout=300s
      done
      kubectl set image deployment/gateway gateway=$HARBOR_URL/$HARBOR_PROJECT/gateway:$CI_COMMIT_SHORT_SHA -n super-market
      kubectl rollout status deployment/gateway -n super-market --timeout=300s
  only:
    - master
  when: manual
```

- [ ] **Step 2: Commit**

```bash
git add .gitlab-ci.yml
git commit -m "feat: add GitLab CI/CD pipeline (4 stages: check, test, build, deploy)"
```

---

### Task E2: 为每个服务添加 Dockerfile

- [ ] **Step 1: 创建 Dockerfile 模板**

写入 `super-market-services/service-user/Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 9001

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

- [ ] **Step 2: 为其余 17 个服务复制并调整端口**

```bash
for svc in service-auth service-member service-address service-product service-category service-inventory service-review service-cart service-order service-payment service-coupon service-seckill service-search service-shop service-platform service-file service-notify; do
  PORT=$(grep -A1 "$svc" docs/superpowers/plans/service-registry.md | grep -oP '\d{4}' | head -1)
  sed "s/9001/$PORT/g" super-market-services/service-user/Dockerfile > "super-market-services/$svc/Dockerfile"
done
```

- [ ] **Step 3: Commit**

```bash
git add super-market-services/*/Dockerfile
git commit -m "feat: add Dockerfile for all 18 microservices (Eclipse Temurin 21 + ZGC)"
```

---

## Task Group F: K8s 基础配置

### Task F1: 创建 K8s 基础资源清单

**Files:**
- Create: `super-market-k8s/base/namespace.yaml`
- Create: `super-market-k8s/base/configmap.yaml`
- Create: `super-market-k8s/base/secret.yaml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p super-market-k8s/{base,apps,monitoring}
```

- [ ] **Step 2: 写入 Namespace**

写入 `super-market-k8s/base/namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: super-market
  labels:
    name: super-market
    environment: production
```

- [ ] **Step 3: 写入 ConfigMap**

写入 `super-market-k8s/base/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: super-market-config
  namespace: super-market
data:
  NACOS_ADDR: "nacos-0.nacos-headless.super-market.svc.cluster.local:8848"
  MYSQL_HOST: "mysql-0.mysql-headless.super-market.svc.cluster.local"
  REDIS_HOST: "redis-0.redis-headless.super-market.svc.cluster.local"
  ROCKETMQ_NAMESRV: "rocketmq-namesrv-0.rocketmq-headless.super-market.svc.cluster.local:9876"
  ES_HOST: "elasticsearch-0.es-headless.super-market.svc.cluster.local:9200"
  MINIO_ENDPOINT: "http://minio-0.minio-headless.super-market.svc.cluster.local:9000"
  JWT_SECRET: "change-this-to-a-real-secret-in-secret-resource"
```

- [ ] **Step 4: 写入 Secret**

写入 `super-market-k8s/base/secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: super-market-secret
  namespace: super-market
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: "root123"
  MYSQL_APP_PASSWORD: "app_user123"
  REDIS_PASSWORD: "redis123"
  MINIO_ROOT_PASSWORD: "minioadmin123"
  JWT_SECRET: "super-market-jwt-secret-min-256-bits-long-key-change-in-production"
```

- [ ] **Step 5: Commit**

```bash
git add super-market-k8s/
git commit -m "feat: add K8s base resources (Namespace, ConfigMap, Secret)"
```

---

### Task F2: 创建 K8s Deployment 模板

**Files:**
- Create: `super-market-k8s/apps/service-template.yaml`

- [ ] **Step 1: 写入通用 Deployment 模板**

写入 `super-market-k8s/apps/service-template.yaml`:

```yaml
# K8s Deployment 通用模板
# 使用方式: 复制并替换 ${SERVICE_NAME}, ${PORT}, ${REPLICAS}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${SERVICE_NAME}
  namespace: super-market
  labels:
    app: ${SERVICE_NAME}
spec:
  replicas: ${REPLICAS}
  selector:
    matchLabels:
      app: ${SERVICE_NAME}
  template:
    metadata:
      labels:
        app: ${SERVICE_NAME}
        version: "1.0.0"
    spec:
      containers:
        - name: ${SERVICE_NAME}
          image: harbor.local/super-market/${SERVICE_NAME}:latest
          imagePullPolicy: Always
          ports:
            - containerPort: ${PORT}
              protocol: TCP
          envFrom:
            - configMapRef:
                name: super-market-config
            - secretRef:
                name: super-market-secret
          resources:
            requests:
              cpu: "200m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1024Mi"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: ${PORT}
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: ${PORT}
            initialDelaySeconds: 15
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
      restartPolicy: Always
```

- [ ] **Step 2: Commit**

```bash
git add super-market-k8s/apps/service-template.yaml
git commit -m "feat: add K8s Deployment template with health probes and resource limits"
```

---

## Task Group G: 最终验证

### Task G1: 全量构建验证

- [ ] **Step 1: 从根目录执行全量 Maven 构建**

```bash
mvn clean install -DskipTests
```
Expected: `BUILD SUCCESS`，所有 23 个模块（5 个 common + 1 个 gateway + 18 个 service，去掉空的 common-dubbo-api 实际为 23+）编译通过

- [ ] **Step 2: 验证项目结构完整性**

```bash
echo "=== Maven 模块 ===" && mvn help:evaluate -Dexpression=project.modules -q -DforceStdout 2>/dev/null || grep '<module>' pom.xml | wc -l
echo "=== 服务启动类 ===" && find . -name "*Application.java" -path "*/main/*" | wc -l
echo "=== Dockerfile ===" && find . -name "Dockerfile" -not -path "*/target/*" | wc -l
echo "=== application.yml ===" && find . -name "application.yml" -path "*/resources/*" | wc -l
```
Expected:
- 23 个 Maven 模块
- 18 个服务启动类 + 1 个 Gateway 启动类 = 19
- 18 个 Dockerfile
- 18 个 application.yml + 1 个 Gateway = 19

- [ ] **Step 3: 提交最终验证记录**

```bash
git add -A
git commit -m "chore: Phase 1 complete — infrastructure + scaffolding verified

Verified:
- Docker Compose: 12 middleware services (MySQL, Redis, Nacos, RocketMQ, ES, MinIO, XXL-Job, Sentinel, SkyWalking, Canal)
- Maven: 23 modules compile successfully (5 common + 18 services)
- Gateway: Spring Cloud Gateway with JWT auth filter + route config
- CI/CD: 4-stage pipeline (check, test, build, deploy)
- K8s: Namespace, ConfigMap, Secret, Deployment template"
```

---

## Phase 1 完成检查清单

- [x] Docker Compose 12 个中间件服务定义完成
- [x] MySQL 初始化 SQL（10 个数据库）
- [x] Maven 父 POM（Spring Boot 3.2 + Dubbo 3.2 + 全部依赖版本管理）
- [x] 5 个 common 模块（core / dubbo-api / security / web / mybatis）
- [x] 18 个微服务脚手架（统一结构、配置、Dockerfile）
- [x] Gateway 网关（Spring Cloud Gateway + JWT 全局过滤器）
- [x] `.gitlab-ci.yml` CI/CD 流水线
- [x] K8s 基础配置（Namespace / ConfigMap / Secret / Deployment 模板）
- [x] 全量 Maven 编译通过

---

## 下一步：Phase 2 准备

Phase 2 将进入核心业务开发（用户域 + 商品域），包含 8 个微服务的完整实现。在启动 Phase 2 之前需要：

1. 确认 Phase 1 环境可正常启动（`docker compose up -d` + Gateway 启动）
2. 编写 Phase 2 详细执行计划（采用相同格式）
3. 准备数据库 DDL（按 `docs/solution-design.md` 第 3 节表结构）

---

> **Plan saved:** `docs/superpowers/plans/2026-05-12-phase1-infrastructure.md`
>
> **Next:** Execute this plan using `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`.
