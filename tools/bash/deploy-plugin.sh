#!/usr/bin/env bash
set -e

if [[ -z "$MINECRAFT_SERVER_PATH" ]]; then
  echo "Error: MINECRAFT_SERVER_PATH environment variable is not set."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

PLUGIN_JAR=$(ls -t "$PROJECT_ROOT"/build/libs/*.jar 2>/dev/null | head -n1)
if [[ ! -f "$PLUGIN_JAR" ]]; then
  echo "Error: No plugin jar found in $PROJECT_ROOT/build/libs. Build the plugin first."
  exit 1
fi

# Optional: Warn if jar is older than any source file
if find "$PROJECT_ROOT/src/main/java" "$PROJECT_ROOT/src/main/resources" -type f -newer "$PLUGIN_JAR" | grep -q .; then
  echo "Warning: The built plugin jar is older than some source files. Consider rebuilding."
fi

mkdir -p "$MINECRAFT_SERVER_PATH/plugins"
cp "$PLUGIN_JAR" "$MINECRAFT_SERVER_PATH/plugins/"
echo "Deployed $PLUGIN_JAR to $MINECRAFT_SERVER_PATH/plugins/" 