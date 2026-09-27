#!/usr/bin/env bash
# Restores a backup made by backup-server.sh into MINECRAFT_SERVER_PATH.
# Each top-level item in the archive (plugins, config, worlds, settings) is
# replaced; a "pre-restore" backup of the current state is taken first.
#
# Usage: restore-server.sh [-y|--yes] [--no-backup] [backup-file|name]
#   Defaults to the newest backup in MINECRAFT_BACKUP_PATH.

set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$TOOLS_DIR/.." && pwd)"
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
MINECRAFT_BACKUP_PATH="${MINECRAFT_BACKUP_PATH:-backups}"
MINECRAFT_BACKUP_PATH="${MINECRAFT_BACKUP_PATH/#\~/$HOME}"
[[ "$MINECRAFT_BACKUP_PATH" == /* ]] || MINECRAFT_BACKUP_PATH="$PROJECT_ROOT/$MINECRAFT_BACKUP_PATH"

ASSUME_YES=false
SAFETY_BACKUP=true
ARCHIVE=""
for arg in "$@"; do
  case "$arg" in
    -y|--yes) ASSUME_YES=true ;;
    --no-backup) SAFETY_BACKUP=false ;;
    -h|--help) sed -n '2,8p' "$0"; exit 0 ;;
    -*) echo "Unknown option: $arg" >&2; exit 1 ;;
    *) ARCHIVE="$arg" ;;
  esac
done

if [[ -z "$ARCHIVE" ]]; then
  ARCHIVE=$(ls -t "$MINECRAFT_BACKUP_PATH"/backup-*.tar.bz2 2>/dev/null | head -n1 || true)
  [[ -n "$ARCHIVE" ]] || { echo "Error: no backups found in $MINECRAFT_BACKUP_PATH." >&2; exit 1; }
elif [[ ! -f "$ARCHIVE" && -f "$MINECRAFT_BACKUP_PATH/$ARCHIVE" ]]; then
  ARCHIVE="$MINECRAFT_BACKUP_PATH/$ARCHIVE"
fi
[[ -f "$ARCHIVE" ]] || { echo "Error: backup not found: $ARCHIVE" >&2; exit 1; }
ARCHIVE="$(cd "$(dirname "$ARCHIVE")" && pwd)/$(basename "$ARCHIVE")"

if pgrep -f "java.*paper-.*\.jar" >/dev/null; then
  echo "Error: the server is running. Stop it first (tools/stop-server.sh)." >&2
  exit 1
fi

ENTRIES=$(tar -tjf "$ARCHIVE")
if grep -qE '^/|(^|/)\.\.(/|$)' <<<"$ENTRIES"; then
  echo "Error: archive contains absolute or '..' paths; refusing to restore." >&2
  exit 1
fi
TOP_LEVEL=$(sed -E 's#^\./##; s#/.*##' <<<"$ENTRIES" | { grep -vE '^\.?$' || true; } | sort -u)
[[ -n "$TOP_LEVEL" ]] || { echo "Error: backup is empty." >&2; exit 1; }

echo "Restore $(basename "$ARCHIVE") into $MINECRAFT_SERVER_PATH"
echo "These items will be replaced:"
sed 's/^/  /' <<<"$TOP_LEVEL"
if ! $ASSUME_YES; then
  read -r -p "Continue? [y/N] " reply
  [[ "$reply" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 1; }
fi

mkdir -p "$MINECRAFT_SERVER_PATH"
if $SAFETY_BACKUP && [[ -n "$(ls -A "$MINECRAFT_SERVER_PATH")" ]]; then
  "$TOOLS_DIR/backup-server.sh" pre-restore
fi

while IFS= read -r item; do
  rm -rf "${MINECRAFT_SERVER_PATH:?}/$item"
done <<<"$TOP_LEVEL"

tar -xjf "$ARCHIVE" -C "$MINECRAFT_SERVER_PATH"
echo "Restored $(basename "$ARCHIVE")."
