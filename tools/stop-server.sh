#!/usr/bin/env bash
# Stops any running Paper server (java process running a paper-*.jar).

set -euo pipefail

STOP_TIMEOUT="${STOP_TIMEOUT:-60}"

SERVER_PIDS=$(pgrep -f "java.*paper-.*\.jar" || true)

if [[ -z "$SERVER_PIDS" ]]; then
  echo "Minecraft server is not running."
  exit 0
fi

for pid in $SERVER_PIDS; do
  echo "Stopping Minecraft server (PID: $pid)..."
  # SIGTERM lets Paper run its shutdown hook and save worlds.
  kill "$pid"
done

for ((i = 0; i < STOP_TIMEOUT; i++)); do
  remaining=""
  for pid in $SERVER_PIDS; do
    kill -0 "$pid" 2>/dev/null && remaining="$remaining $pid"
  done
  if [[ -z "$remaining" ]]; then
    echo "Minecraft server stopped."
    exit 0
  fi
  sleep 1
done

echo "Server did not stop within ${STOP_TIMEOUT}s; forcing:$remaining"
kill -9 $remaining
