#!/usr/bin/env bash
# Backs up plugins, config, worlds, and settings from MINECRAFT_SERVER_PATH
# to MINECRAFT_BACKUP_PATH/backup-<timestamp>[-label].tar.bz2.
#
# Usage: backup-server.sh [-f|--force] [label]
#   -f  Back up even if the server is running (worlds may be inconsistent).

set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=setup-env.sh
source "$TOOLS_DIR/setup-env.sh"

FORCE=false
LABEL=""
for arg in "$@"; do
  case "$arg" in
    -f|--force) FORCE=true ;;
    -h|--help) sed -n '2,7p' "$0"; exit 0 ;;
    -*) echo "Unknown option: $arg" >&2; exit 1 ;;
    *) LABEL="$arg" ;;
  esac
done

[[ -d "$MINECRAFT_SERVER_PATH" ]] || { echo "Error: $MINECRAFT_SERVER_PATH does not exist." >&2; exit 1; }

if pgrep -f "java.*paper-.*\.jar" >/dev/null && ! $FORCE; then
  echo "Error: the server is running. Stop it first (tools/stop-server.sh) or pass --force." >&2
  exit 1
fi

cd "$MINECRAFT_SERVER_PATH"
shopt -s nullglob

ITEMS=()
[[ -d plugins ]] && ITEMS+=(plugins)
[[ -d config ]] && ITEMS+=(config)
for dir in */; do
  dir="${dir%/}"
  [[ -f "$dir/level.dat" ]] && ITEMS+=("$dir")
done
for f in *.yml *.yaml *.json *.properties eula.txt; do
  ITEMS+=("$f")
done

if (( ${#ITEMS[@]} == 0 )); then
  echo "Error: nothing to back up in $MINECRAFT_SERVER_PATH." >&2
  exit 1
fi

# Keep labels filename-safe.
LABEL="${LABEL//[^A-Za-z0-9._-]/_}"
NAME="backup-$(date +%Y%m%d-%H%M%S)${LABEL:+-$LABEL}.tar.bz2"
mkdir -p "$MINECRAFT_BACKUP_PATH"
ARCHIVE="$MINECRAFT_BACKUP_PATH/$NAME"

echo "Backing up: ${ITEMS[*]}"
trap 'rm -f "$ARCHIVE.part"' EXIT
tar --exclude 'plugins/.paper-remapped' --exclude '*/session.lock' \
  -cjf "$ARCHIVE.part" "${ITEMS[@]}"
mv "$ARCHIVE.part" "$ARCHIVE"
trap - EXIT

echo "Created $ARCHIVE ($(du -h "$ARCHIVE" | awk '{print $1}'))"
