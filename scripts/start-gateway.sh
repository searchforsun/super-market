#!/bin/bash
# ============================================================
# Start the API Gateway only (256MB heap + SerialGC)
# ============================================================
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# Load environment variables (middleware-docker/.env first, with defaults)
ENV_FILE="$PROJECT_ROOT/middleware-docker/.env"
if [ -f "$ENV_FILE" ]; then set -a; source "$ENV_FILE"; set +a; fi

# Connection host
export DOCKER_HOST_IP="${DOCKER_HOST_IP:-localhost}"

# Nacos
export NACOS_PORT="${NACOS_PORT:-8848}"
export NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
export NACOS_PASSWORD="${NACOS_PASSWORD:-nacos123}"

# MySQL
export MYSQL_PORT="${MYSQL_PORT:-3306}"
export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123}"

# Redis
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-redis123}"

# RocketMQ
export ROCKETMQ_NAMESRV_PORT="${ROCKETMQ_NAMESRV_PORT:-9876}"

# Elasticsearch
export ES01_PORT="${ES01_PORT:-9200}"

# Seata
export SEATA_PORT="${SEATA_PORT:-8091}"

echo "Starting Gateway (256MB, SerialGC) ..."
mvn -pl super-market-gateway spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx256m -Xms128m -XX:+UseSerialGC" \
  &> /tmp/smt-super-market-gateway.log &

echo "Gateway PID: $!"
echo "Swagger UI will be available at: http://localhost:8999/doc.html"
