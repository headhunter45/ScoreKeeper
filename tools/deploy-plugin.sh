#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ ! -r "$PROJECT_ROOT/.env" ]]; then
  echo "Error: $PROJECT_ROOT/.env is missing or unreadable. Copy .env.example to .env and set its values." >&2
  exit 1
fi
load_project_env() {
  local env_line env_key env_value first_char last_char
  while IFS= read -r env_line || [[ -n "$env_line" ]]; do
    env_line="${env_line%$'\r'}"
    [[ "$env_line" =~ ^[[:space:]]*(#|$) ]] && continue
    [[ "$env_line" == *=* ]] || continue

    env_key="${env_line%%=*}"
    env_value="${env_line#*=}"
    env_key="${env_key#"${env_key%%[![:space:]]*}"}"
    env_key="${env_key%"${env_key##*[![:space:]]}"}"
    [[ "$env_key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    env_value="${env_value#"${env_value%%[![:space:]]*}"}"
    env_value="${env_value%"${env_value##*[![:space:]]}"}"

    if (( ${#env_value} >= 2 )); then
      first_char="${env_value:0:1}"
      last_char="${env_value:$((${#env_value} - 1)):1}"
      if [[ "$first_char" == "$last_char" && ( "$first_char" == '"' || "$first_char" == "'" ) ]]; then
        env_value="${env_value:1:$((${#env_value} - 2))}"
      fi
    fi

    if [[ -z "${!env_key:-}" ]]; then
      export "$env_key=$env_value"
    fi
  done < "$PROJECT_ROOT/.env"
}
load_project_env

MINECRAFT_SERVER_PATH="${MINECRAFT_SERVER_PATH:-MCServer}"
MINECRAFT_SERVER_PATH="${MINECRAFT_SERVER_PATH/#\~/$HOME}"
[[ "$MINECRAFT_SERVER_PATH" == /* ]] || MINECRAFT_SERVER_PATH="$PROJECT_ROOT/$MINECRAFT_SERVER_PATH"

if [[ -z "$MINECRAFT_SERVER_PATH" ]]; then
  echo "Error: MINECRAFT_SERVER_PATH environment variable is not set."
  exit 1
fi

PLUGIN_JAR=$(ls -t "$PROJECT_ROOT"/build/libs/*.jar 2>/dev/null | grep -v -- '-all\.jar$' | head -n1)
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