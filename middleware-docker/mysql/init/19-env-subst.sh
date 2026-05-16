#!/bin/bash
# ============================================
# 环境变量替换脚本
# 在 20-seed-data.sql 执行前，将占位符替换为
# middleware-docker 传入的环境变量值（来自 .env）
# ============================================
set -e

TARGET="/docker-entrypoint-initdb.d/20-seed-data.sql"
HOST_IP="${DOCKER_HOST_IP:-localhost}"
MINIO_PORT="${MINIO_API_PORT:-9000}"

if [ -f "$TARGET" ]; then
  sed -i "s/__DOCKER_HOST_IP__/${HOST_IP}/g; s/__MINIO_API_PORT__/${MINIO_PORT}/g" "$TARGET"
  echo "Env substitution done: DOCKER_HOST_IP=$HOST_IP MINIO_API_PORT=$MINIO_PORT"
else
  echo "WARNING: $TARGET not found, skipping env substitution"
fi
