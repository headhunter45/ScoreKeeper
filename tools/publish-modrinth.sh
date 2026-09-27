#!/usr/bin/env bash
set -euo pipefail

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

# Required env:
#   MODRINTH_TOKEN
#   MODRINTH_PROJECT_ID (project ID or slug)
#   MODRINTH_GAME_VERSIONS (optional comma-separated list; defaults to 1.21 through 1.21.7)
#   MODRINTH_LOADERS (optional, comma-separated, default paper)
#   MODRINTH_CHANGELOG (optional)
# Optional:
#   MODRINTH_DEPENDENCIES (optional JSON array or a single JSON string)
#   MODRINTH_FILE (optional path to the jar; defaults to build/libs/*.jar)

MODRINTH_TOKEN="${MODRINTH_TOKEN:-}"
MODRINTH_PROJECT_ID="${MODRINTH_PROJECT_ID:-5NYwxysp}"
generate_changelog() {
  local current_tag previous_tag range commits
  current_tag=$(git -C "$PROJECT_ROOT" describe --tags --exact-match HEAD 2>/dev/null || true)
  if [[ -n "$current_tag" ]]; then
    previous_tag=$(git -C "$PROJECT_ROOT" describe --tags --abbrev=0 "${current_tag}^" 2>/dev/null || true)
    range="${previous_tag:+$previous_tag..}$current_tag"
  else
    previous_tag=$(git -C "$PROJECT_ROOT" describe --tags --abbrev=0 2>/dev/null || true)
    range="${previous_tag:+$previous_tag..}HEAD"
  fi
  commits=$(git -C "$PROJECT_ROOT" log --no-merges --format='- %s (%h)' "$range" 2>/dev/null || true)
  if [[ -z "$commits" ]]; then
    commits='- No commit messages found for this release.'
  fi
  printf '%s\n\n%s\n' "## What's Changed" "$commits"
}
MODRINTH_CHANGELOG="${MODRINTH_CHANGELOG:-$(generate_changelog)}"
MODRINTH_GAME_VERSIONS="${MODRINTH_GAME_VERSIONS:-26.2,26.3}"
MODRINTH_LOADERS="${MODRINTH_LOADERS:-paper}"
MODRINTH_VERSION_TYPE="${MODRINTH_VERSION_TYPE:-}"
MODRINTH_FILE="${MODRINTH_FILE:-}"
MODRINTH_DEPENDENCIES="${MODRINTH_DEPENDENCIES:-}"

MODRINTH_VERSION=$(
  "$PROJECT_ROOT/gradlew" -q -p "$PROJECT_ROOT" properties --property version \
    | sed -n 's/^version: //p'
)
if [[ -z "$MODRINTH_VERSION" ]]; then
  echo "Error: could not determine the project version from Gradle." >&2
  exit 1
fi
if [[ -z "$MODRINTH_VERSION_TYPE" ]]; then
  if [[ "$MODRINTH_VERSION" == *-* ]]; then
    MODRINTH_VERSION_TYPE="beta"
  else
    MODRINTH_VERSION_TYPE="release"
  fi
fi
case "$MODRINTH_VERSION_TYPE" in
  release|beta|alpha) ;;
  *) echo "Error: MODRINTH_VERSION_TYPE must be release, beta, or alpha." >&2; exit 1 ;;
esac
MODRINTH_VERSION_NAME="ScoreKeeper $MODRINTH_VERSION"

if [[ -n "$MODRINTH_FILE" && "$MODRINTH_FILE" != /* ]]; then
  MODRINTH_FILE="$PROJECT_ROOT/$MODRINTH_FILE"
fi

if [[ -z "$MODRINTH_TOKEN" ]]; then
  echo "Error: MODRINTH_TOKEN is not set in $PROJECT_ROOT/.env" >&2
  exit 1
fi
if [[ -z "$MODRINTH_FILE" ]]; then
  MODRINTH_FILE=$(ls -t "$PROJECT_ROOT"/build/libs/*.jar 2>/dev/null | grep -v -- '-all\.jar$' | head -n1 || true)
fi
if [[ -z "$MODRINTH_FILE" || ! -f "$MODRINTH_FILE" ]]; then
  echo "Error: no jar found for Modrinth upload. Build the project first or set MODRINTH_FILE." >&2
  exit 1
fi
if ! command -v unzip >/dev/null; then
  echo "Error: 'unzip' is required to verify the plugin version in the jar." >&2
  exit 1
fi
JAR_VERSION=$(unzip -p "$MODRINTH_FILE" plugin.yml 2>/dev/null | sed -n 's/^version: //p' | head -n1 || true)
if [[ -z "$JAR_VERSION" ]]; then
  echo "Error: could not read the plugin version from $MODRINTH_FILE." >&2
  exit 1
fi
if [[ "$JAR_VERSION" != "$MODRINTH_VERSION" ]]; then
  echo "Error: jar version $JAR_VERSION does not match Gradle version $MODRINTH_VERSION. Rebuild the plugin and retry." >&2
  exit 1
fi

csv_to_json_array() {
  jq -cn --arg csv "$1" '$csv | split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))'
}
MODRINTH_GAME_VERSIONS_JSON=$(csv_to_json_array "$MODRINTH_GAME_VERSIONS")
MODRINTH_LOADERS_JSON=$(csv_to_json_array "$MODRINTH_LOADERS")
if [[ -n "$MODRINTH_DEPENDENCIES" ]]; then
  if ! MODRINTH_DEPENDENCIES_JSON=$(jq -ce 'if type == "array" then . else error("dependencies must be a JSON array") end' <<<"$MODRINTH_DEPENDENCIES"); then
    echo "Error: MODRINTH_DEPENDENCIES must be a JSON array." >&2
    exit 1
  fi
else
  MODRINTH_DEPENDENCIES_JSON='[]'
fi

MODRINTH_DATA=$(jq -cn \
  --arg project_id "$MODRINTH_PROJECT_ID" \
  --arg name "$MODRINTH_VERSION_NAME" \
  --arg version_number "$MODRINTH_VERSION" \
  --arg version_type "$MODRINTH_VERSION_TYPE" \
  --arg changelog "$MODRINTH_CHANGELOG" \
  --argjson featured false \
  --argjson dependencies "$MODRINTH_DEPENDENCIES_JSON" \
  --argjson game_versions "$MODRINTH_GAME_VERSIONS_JSON" \
  --argjson loaders "$MODRINTH_LOADERS_JSON" \
  '{project_id: $project_id, name: $name, version_number: $version_number, version_type: $version_type, featured: $featured, changelog: $changelog, dependencies: $dependencies, game_versions: $game_versions, loaders: $loaders, file_parts: ["file"], primary_file: "file"}')

CURL_ARGS=(
  -X POST "https://api.modrinth.com/v2/version"
  -H "Authorization: $MODRINTH_TOKEN"
  -F "data=$MODRINTH_DATA;type=application/json"
  -F "file=@$MODRINTH_FILE"
)

HTTP_RESPONSE=$(curl -sS -w $'\n%{http_code}' "${CURL_ARGS[@]}")
HTTP_STATUS="${HTTP_RESPONSE##*$'\n'}"
HTTP_BODY="${HTTP_RESPONSE%$'\n'*}"
if [[ "$HTTP_STATUS" != 2?? ]]; then
  echo "Error: Modrinth API returned HTTP $HTTP_STATUS:" >&2
  if ! printf '%s\n' "$HTTP_BODY" | jq -r '.description // .error // .' >&2; then
    printf '%s\n' "$HTTP_BODY" >&2
  fi
  exit 1
fi
printf '%s\n' "$HTTP_BODY" | jq .
