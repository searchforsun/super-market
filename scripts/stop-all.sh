#!/bin/bash
# ============================================================
# Stop all running super-market backend services
# ============================================================

echo "Stopping all super-market services ..."

# Find all Java processes running super-market Application classes
PIDS=$(jps -l 2>/dev/null | grep "com.supermarket.*Application$" | awk '{print $1}')

if [ -z "$PIDS" ]; then
  echo "No super-market services found running."
  exit 0
fi

COUNT=0
for pid in $PIDS; do
  app_name=$(jps -l 2>/dev/null | grep "^$pid " | awk '{print $2}' | sed 's/com.supermarket.//' | sed 's/.Application//')
  echo "  Stopping $app_name (PID $pid) ..."
  kill "$pid" 2>/dev/null && COUNT=$((COUNT + 1))
done

echo "Stopped $COUNT services."
