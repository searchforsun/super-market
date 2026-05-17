#!/bin/bash
# ============================================================
# Start all 19 backend services (18 microservices + gateway)
# Each service: 128MB heap, gateway: 256MB + SerialGC
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

# ============================================================
# Phase 1 — Launch all services
# ============================================================
echo "=========================================="
echo "  Phase 1: Launching all 19 services"
echo "=========================================="

declare -A PIDS
declare -A PORTS
declare -A APP_NAMES

for svc in "${SERVICES[@]}"; do
  svc_name=$(basename "$svc")

  # JVM settings
  if [ "$svc_name" = "super-market-gateway" ]; then
    JVM_ARGS="-Xmx256m -Xms128m -XX:+UseSerialGC"
    BOOTSTRAP="$svc/src/main/resources/bootstrap.yml"
  else
    JVM_ARGS="-Xmx128m -Xms64m -XX:+UseSerialGC"
    BOOTSTRAP="$svc/src/main/resources/bootstrap.yml"
  fi

  # Extract identity from bootstrap.yml
  APP_NAME=$(awk '/^    name:/ {print $2; exit}' "$BOOTSTRAP" 2>/dev/null || echo "")
  PORT=$(grep -oP 'SERVER_PORT:\K\d+' "$BOOTSTRAP" 2>/dev/null || echo "0")

  APP_NAMES["$svc_name"]="$APP_NAME"
  PORTS["$svc_name"]="$PORT"

  echo -n "  Launching ${APP_NAME:-$svc_name} (port ${PORT:-?}) ... "

  mvn -pl "$svc" spring-boot:run \
    -Dspring-boot.run.profiles="${SPRING_PROFILES_ACTIVE}" \
    -Dspring-boot.run.jvmArguments="${JVM_ARGS}" \
    &> "/tmp/smt-${svc_name}.log" &

  PID=$!
  PIDS["$svc_name"]=$PID
  echo "PID $PID"

  sleep 8
done

echo ""
echo "All 19 launched. Waiting 30s for initial bootstrap ..."
sleep 30

# ============================================================
# Phase 2 — Verify each service
# ============================================================
echo ""
echo "=========================================="
echo "  Phase 2: Verifying health + Nacos"
echo "=========================================="

MAX_WAIT=120
declare -A RESULTS

for svc in "${SERVICES[@]}"; do
  svc_name=$(basename "$svc")
  APP_NAME="${APP_NAMES[$svc_name]}"
  PORT="${PORTS[$svc_name]}"
  PID="${PIDS[$svc_name]}"

  printf "  %-20s  " "$APP_NAME"

  WAITED=0
  HEALTH_OK=false
  NACOS_OK=false

  while [ $WAITED -lt $MAX_WAIT ]; do
    sleep 5
    WAITED=$((WAITED + 5))

    # Process alive?
    if ! kill -0 $PID 2>/dev/null; then
      RESULTS["$svc_name"]="DIED"
      break
    fi

    # Health check
    if [ "$HEALTH_OK" = false ]; then
      HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" 2>/dev/null || echo "000")
      if [ "$HEALTH_CODE" = "200" ]; then
        HEALTH_RESP=$(curl -s "http://localhost:${PORT}/actuator/health" 2>/dev/null)
        if echo "$HEALTH_RESP" | grep -q '"UP"'; then
          HEALTH_OK=true
        fi
      fi
    fi

    # Nacos registration (login first, then query — reuse token across services)
    if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = false ]; then
      if [ -z "$NACOS_TOKEN" ]; then
        NACOS_TOKEN=$(curl -s -X POST "http://${DOCKER_HOST_IP}:${NACOS_PORT}/nacos/v1/auth/login" \
          -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" 2>/dev/null | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
      fi
      if [ -n "$NACOS_TOKEN" ]; then
        NACOS_RESP=$(curl -s "http://${DOCKER_HOST_IP}:${NACOS_PORT}/nacos/v1/ns/instance/list?serviceName=${APP_NAME}&namespaceId=public&accessToken=${NACOS_TOKEN}" 2>/dev/null)
        if echo "$NACOS_RESP" | grep -q '"healthy":true'; then
          NACOS_OK=true
        fi
      elif grep -aq "nacos.*registry.*register\|register to nacos\|naming.*register" "/tmp/smt-${svc_name}.log" 2>/dev/null; then
        NACOS_OK=true
      fi
    fi

    if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = true ]; then
      break
    fi
  done

  # Record result
  if [ "$HEALTH_OK" = true ] && [ "$NACOS_OK" = true ]; then
    RESULTS["$svc_name"]="OK"
    echo "OK (${WAITED}s)"
  elif [ "${RESULTS[$svc_name]}" = "DIED" ]; then
    echo "DIED"
  elif [ "$HEALTH_OK" = true ]; then
    RESULTS["$svc_name"]="NO_NACOS"
    echo "UP (Nacos unconfirmed)"
  else
    RESULTS["$svc_name"]="TIMEOUT"
    echo "TIMEOUT (health: $([ "$HEALTH_OK" = true ] && echo UP || echo DOWN))"
  fi
done

# ============================================================
# Phase 3 — Retry DIED services (up to 2 retries each)
# ============================================================
RETRY_MAX=2
for svc in "${SERVICES[@]}"; do
  svc_name=$(basename "$svc")
  if [ "${RESULTS[$svc_name]}" != "DIED" ]; then
    continue
  fi

  APP_NAME="${APP_NAMES[$svc_name]}"
  svc_path="$svc"

  for attempt in $(seq 1 $RETRY_MAX); do
    echo ""
    echo "  Retrying $APP_NAME (attempt $attempt/$RETRY_MAX) ..."

    "$PROJECT_ROOT/scripts/start-service.sh" "${svc_path#super-market-services/}" 2>/dev/null && {
      RESULTS["$svc_name"]="OK"
      break
    } || {
      echo "  Retry $attempt failed for $APP_NAME"
    }
  done
done

# ============================================================
# Summary
# ============================================================
echo ""
echo "=========================================="
echo "  Startup Summary"
echo "=========================================="

OK_COUNT=0
FAIL_COUNT=0

for svc in "${SERVICES[@]}"; do
  svc_name=$(basename "$svc")
  APP_NAME="${APP_NAMES[$svc_name]}"
  RESULT="${RESULTS[$svc_name]}"

  case "$RESULT" in
    OK)
      printf "  [OK]       %s\n" "$APP_NAME"
      OK_COUNT=$((OK_COUNT + 1))
      ;;
    NO_NACOS)
      printf "  [NO_NACOS] %s (health UP, Nacos not confirmed)\n" "$APP_NAME"
      OK_COUNT=$((OK_COUNT + 1))
      ;;
    DIED)
      printf "  [DIED]     %s (check /tmp/smt-%s.log)\n" "$APP_NAME" "$svc_name"
      FAIL_COUNT=$((FAIL_COUNT + 1))
      ;;
    TIMEOUT)
      printf "  [TIMEOUT]  %s (health check failed)\n" "$APP_NAME"
      FAIL_COUNT=$((FAIL_COUNT + 1))
      ;;
  esac
done

echo ""
echo "Total: $OK_COUNT OK, $FAIL_COUNT failed"
echo "Swagger UI: http://localhost:8999/doc.html"
