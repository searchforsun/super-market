#!/bin/bash
# ============================================================
# Stop all running frontend Vite dev servers
# ============================================================

echo "Stopping all frontend dev servers ..."

# Try to find and kill vite/node processes on ports 5173-5175
COUNT=0

# Method 1: ps + grep (works on Git Bash / Linux / macOS)
PIDS=$(ps aux 2>/dev/null | grep -E "vite" | grep -v grep | awk '{print $1}')
for pid in $PIDS; do
  echo "  Stopping vite process (PID $pid) ..."
  kill "$pid" 2>/dev/null && COUNT=$((COUNT + 1))
done

# Method 2: netstat on Windows (fallback)
if [ $COUNT -eq 0 ]; then
  for port in 5173 5174 5175; do
    PID=$(netstat -ano 2>/dev/null | grep ":$port " | grep LISTENING | awk '{print $5}' | head -1)
    if [ -n "$PID" ] && [ "$PID" != "0" ]; then
      echo "  Stopping process on port $port (PID $PID) ..."
      taskkill //PID "$PID" //F 2>/dev/null && COUNT=$((COUNT + 1))
    fi
  done
fi

if [ $COUNT -eq 0 ]; then
  echo "No frontend dev servers found running."
else
  echo "Stopped $COUNT frontend server(s)."
fi
