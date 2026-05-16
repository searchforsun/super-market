#!/bin/bash
# ============================================================
# Start all 19 backend services (18 microservices + gateway)
# Each service: 128MB heap, gateway: 256MB + SerialGC
# Logs: ./logs/<service-name>/
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

SERVICES=(
  "super-market-gateway"
  "super-market-services/service-user"
  "super-market-services/service-auth"
  "super-market-services/service-member"
  "super-market-services/service-address"
  "super-market-services/service-product"
  "super-market-services/service-category"
  "super-market-services/service-inventory"
  "super-market-services/service-review"
  "super-market-services/service-cart"
  "super-market-services/service-order"
  "super-market-services/service-payment"
  "super-market-services/service-coupon"
  "super-market-services/service-seckill"
  "super-market-services/service-search"
  "super-market-services/service-shop"
  "super-market-services/service-platform"
  "super-market-services/service-file"
  "super-market-services/service-notify"
)

echo "=========================================="
echo "  Starting all services (low memory mode)"
echo "=========================================="

for svc in "${SERVICES[@]}"; do
  svc_name=$(basename "$svc")

  if [ "$svc_name" = "super-market-gateway" ]; then
    JVM_ARGS="-Xmx256m -Xms128m -XX:+UseSerialGC"
  else
    JVM_ARGS="-Xmx128m -Xms64m -XX:+UseSerialGC"
  fi

  echo -n "  Starting ${svc_name} ... "
  mvn -pl "$svc" spring-boot:run \
    -Dspring-boot.run.jvmArguments="${JVM_ARGS}" \
    &> "/tmp/smt-${svc_name}.log" &
  echo "PID $!"
  sleep 5
done

echo ""
echo "All 19 services launched. Check status with: jps -l | grep Application"
echo "Swagger UI: http://localhost:8999/doc.html"
