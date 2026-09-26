#!/usr/bin/env bash
# Restores a backup made by backup-server.sh into MINECRAFT_SERVER_PATH.
# Each top-level item in the archive (plugins, config, worlds, settings) is
# replaced; a "pre-restore" backup of the current state is taken first.
#
# Usage: restore-server.sh [-y|--yes] [--no-backup] [backup-file|name]
#   Defaults to the newest backup in MINECRAFT_BACKUP_PATH.

set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=setup-env.sh
source "$TOOLS_DIR/setup-env.sh"

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
