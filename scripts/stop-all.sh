#!/bin/bash
# ============================================================
# Stop all running super-market backend services
# ============================================================

echo "Stopping all super-market services ..."

PIDS=$(jps -l 2>/dev/null | grep "com.supermarket.*Application$" | awk '{print $1}')

if [ -z "$PIDS" ]; then
  echo "No super-market services found running."
  exit 0
fi

COUNT=0
for pid in $PIDS; do
  app_name=$(jps -l 2>/dev/null | grep "^$pid " | awk '{print $2}' | sed 's/com.supermarket.//' | sed 's/.Application//')
  echo -n "  Stopping $app_name (PID $pid) ... "

  # Try Windows taskkill first, fall back to Unix kill
  if command -v taskkill &>/dev/null; then
    taskkill //PID "$pid" //F &>/dev/null && { echo "OK"; COUNT=$((COUNT + 1)); } || echo "FAILED"
  else
    kill "$pid" 2>/dev/null && { echo "OK"; COUNT=$((COUNT + 1)); } || echo "FAILED"
  fi
done

sleep 2

REMAINING=$(jps -l 2>/dev/null | grep -c "com.supermarket.*Application" | tr -d '\n\r' || echo "0")
REMAINING=${REMAINING:-0}
echo "Stopped $COUNT services ($REMAINING remaining)."

if [ "$REMAINING" -gt 0 ]; then
  echo "Some processes may still be alive — check with: jps -l | grep Application"
fi
