#!/usr/bin/env bash

set -e

if [[ -z "$MINECRAFT_SERVER_PATH" || -z "$MINECRAFT_SERVER_JAR" ]]; then
  echo "Error: MINECRAFT_SERVER_PATH and MINECRAFT_SERVER_JAR must be set."
  exit 1
fi

cd "$MINECRAFT_SERVER_PATH"

SERVER_PID=$(pgrep -f "java.*$MINECRAFT_SERVER_JAR" || true)

if [[ -n "$SERVER_PID" ]]; then
  echo "Minecraft server is running (PID: $SERVER_PID). Stopping it..."
  kill "$SERVER_PID"
  sleep 5
  echo "Minecraft server stopped."
else
  echo "Minecraft server is not running."
fi
