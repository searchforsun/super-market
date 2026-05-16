#!/bin/bash
# ============================================================
# Start frontend app(s)
# Usage: ./start-frontend.sh <app-name|all>
#   e.g. ./start-frontend.sh app-admin
#        ./start-frontend.sh app-b2b
#        ./start-frontend.sh app-b2c
#        ./start-frontend.sh all
# ============================================================
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT/frontend"

if [ -z "$1" ]; then
  echo "Usage: $0 <app-name|all>"
  echo "  e.g. $0 app-admin"
  echo "       $0 app-b2b"
  echo "       $0 app-b2c"
  echo "       $0 all"
  exit 1
fi

start_app() {
  local app="$1"
  if [ ! -d "$app" ]; then
    echo "Error: App '$app' not found in frontend/"
    return 1
  fi
  echo "  Starting $app ..."
  cd "$PROJECT_ROOT/frontend/$app"
  pnpm dev &> "/tmp/smt-${app}.log" &
  echo "    PID $!  (logs: /tmp/smt-${app}.log)"
  cd "$PROJECT_ROOT/frontend"
}

if [ "$1" = "all" ]; then
  echo "=========================================="
  echo "  Starting all frontend apps"
  echo "=========================================="
  start_app "app-admin"
  start_app "app-b2b"
  start_app "app-b2c"
  echo ""
  echo "All frontend apps launched:"
  echo "  Admin:  http://localhost:5175"
  echo "  B2B:    http://localhost:5174"
  echo "  B2C:    http://localhost:5173"
else
  start_app "$1"
fi
