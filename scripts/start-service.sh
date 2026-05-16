#!/bin/bash
# ============================================================
# Start a single backend service (128MB heap + SerialGC)
# Usage: ./start-service.sh <service-name>
#   e.g. ./start-service.sh service-user
#        ./start-service.sh service-order
# ============================================================
set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <service-name>"
  echo "  e.g. $0 service-user"
  echo "       $0 service-order"
  echo ""
  echo "Available services:"
  echo "  service-user       service-auth        service-member"
  echo "  service-address    service-product     service-category"
  echo "  service-inventory  service-review      service-cart"
  echo "  service-order      service-payment     service-coupon"
  echo "  service-seckill    service-search      service-shop"
  echo "  service-platform   service-file        service-notify"
  exit 1
fi

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

SERVICE="super-market-services/$1"

if [ ! -d "$SERVICE" ]; then
  echo "Error: Service '$1' not found at $SERVICE"
  exit 1
fi

echo "Starting $1 (128MB, SerialGC) ..."
mvn -pl "$SERVICE" spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx128m -Xms64m -XX:+UseSerialGC" \
  &> "/tmp/smt-$(basename "$SERVICE").log" &

echo "PID: $!"
echo "API docs: http://localhost:8999/api/$(echo $1 | sed 's/service-//')/v3/api-docs"
