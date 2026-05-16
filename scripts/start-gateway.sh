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
export NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

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

# Spring profiles
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

APP_NAME="gateway-service"
PORT=8999
JVM_ARGS="-Xmx256m -Xms128m -XX:+UseSerialGC"

echo "=== Starting $APP_NAME (port $PORT) ==="
echo "    JVM: $JVM_ARGS"

mvn -pl super-market-gateway spring-boot:run \
  -Dspring-boot.run.profiles="${SPRING_PROFILES_ACTIVE}" \
  -Dspring-boot.run.jvmArguments="${JVM_ARGS}" \
  &> /tmp/smt-super-market-gateway.log &

PID=$!
echo "    PID: $PID"

# --- Wait for startup verification ---
MAX_WAIT=120
INTERVAL=5
WAITED=0
HEALTH_OK=false
NACOS_OK=false

while [ $WAITED -lt $MAX_WAIT ]; do
  sleep $INTERVAL
  WAITED=$((WAITED + INTERVAL))

  # Process alive?
  if ! kill -0 $PID 2>/dev/null; then
    echo "    FAILED — process exited (check /tmp/smt-super-market-gateway.log)"
    exit 1
  fi

  # Health check
  if [ "$HEALTH_OK" = false ]; then
    HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" 2>/dev/null || echo "000")
    if [ "$HEALTH_CODE" = "200" ]; then
      HEALTH_RESP=$(curl -s "http://localhost:${PORT}/actuator/health" 2>/dev/null)
      if echo "$HEALTH_RESP" | grep -q '"UP"'; then
        HEALTH_OK=true
        echo "    [${WAITED}s] health: UP"
      fi
    fi
  fi

  # Nacos registration (login first, then query)
  if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = false ]; then
    NACOS_TOKEN=$(curl -s -X POST "http://${DOCKER_HOST_IP}:${NACOS_PORT}/nacos/v1/auth/login" \
      -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" 2>/dev/null | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$NACOS_TOKEN" ]; then
      NACOS_RESP=$(curl -s "http://${DOCKER_HOST_IP}:${NACOS_PORT}/nacos/v1/ns/instance/list?serviceName=${APP_NAME}&namespaceId=public&accessToken=${NACOS_TOKEN}" 2>/dev/null)
      if echo "$NACOS_RESP" | grep -q '"healthy":true'; then
        NACOS_OK=true
        echo "    [${WAITED}s] Nacos: registered"
      fi
    elif grep -aq "nacos.*registry.*register\|register to nacos\|naming.*register" /tmp/smt-super-market-gateway.log 2>/dev/null; then
      NACOS_OK=true
      echo "    [${WAITED}s] Nacos: registered (log)"
    fi
  fi

  if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = true ]; then
    break
  fi
done

if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = true ]; then
  echo "=== $APP_NAME started successfully (${WAITED}s) ==="
  echo "    Swagger UI: http://localhost:8999/doc.html"
else
  echo "=== $APP_NAME startup incomplete after ${MAX_WAIT}s ==="
  [ "$HEALTH_OK" = false ] && echo "    Health check: FAILED"
  [ "$NACOS_OK" = false ] && echo "    Nacos registration: UNCONFIRMED"
  exit 1
fi
