#!/usr/bin/env bash

set -e

# Check required environment variables
if [[ -z "$MINECRAFT_SERVER_PATH" || -z "$MINECRAFT_SERVER_JAR" ]]; then
  echo "Error: MINECRAFT_SERVER_PATH and MINECRAFT_SERVER_JAR must be set."
  exit 1
fi

cd "$MINECRAFT_SERVER_PATH"

# Find running server process (java with the server jar)
SERVER_PID=$(pgrep -f "java.*$MINECRAFT_SERVER_JAR" || true)

if [[ -n "$SERVER_PID" ]]; then
  echo "Minecraft server is running (PID: $SERVER_PID). Stopping it..."
  kill "$SERVER_PID"
  sleep 5
else
  echo "Minecraft server is not running."
fi

# Start the server (in a detached screen session)
pushd "${MINECRAFT_SERVER_PATH}"
java -jar "${MINECRAFT_SERVER_JAR}"
popd
