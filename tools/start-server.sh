#!/usr/bin/env bash
# Downloads (if missing) and runs a Paper server via the Fill v3 API.
# Server settings come from ../.env relative to this script (see setup-env.sh).
#
# Env vars:
#   JAVA_OPTS              Extra JVM args (default: -Xms2G -Xmx2G)

set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=setup-env.sh
source "$TOOLS_DIR/setup-env.sh"

[[ -n "$PAPER_VERSION" ]] || { echo "Error: PAPER_VERSION is not set (see .env)." >&2; exit 1; }

PAPER_PROJECT="paper"
JAVA_OPTS="${JAVA_OPTS:--Xms2G -Xmx2G}"
FILL_API="https://fill.papermc.io/v3/projects/${PAPER_PROJECT}/versions/${PAPER_VERSION}"
# Fill requires a non-generic User-Agent with contact info.
USER_AGENT="${PAPER_USER_AGENT:-majinnaibu-plugin-dev/1.0 (https://github.com/majinnaibu)}"

for cmd in curl jq java shasum; do
  command -v "$cmd" >/dev/null || { echo "Error: '$cmd' is required." >&2; exit 1; }
done

api_get() {
  curl -fsSL -H "User-Agent: $USER_AGENT" "$1"
}

mkdir -p "$MINECRAFT_SERVER_PATH"
cd "$MINECRAFT_SERVER_PATH"

JAR_BUILD="${MINECRAFT_SERVER_JAR:-}"
JAR_BUILD="${JAR_BUILD#paper-"$PAPER_VERSION"-}"; JAR_BUILD="${JAR_BUILD%.jar}"
if [[ -z "$PAPER_BUILD" && "$JAR_BUILD" =~ ^[0-9]+$ ]]; then
  PAPER_BUILD="$JAR_BUILD"
fi

if [[ -z "$PAPER_BUILD" ]]; then
  echo "Resolving latest stable build for Paper $PAPER_VERSION..."
  PAPER_BUILD=$(api_get "$FILL_API/builds" \
    | jq -r 'map(select(.channel == "STABLE")) | max_by(.id) | .id // empty') || true
  if [[ -z "$PAPER_BUILD" ]]; then
    # Offline or no stable build: fall back to the newest local jar for this version.
    for f in paper-"$PAPER_VERSION"-*.jar; do
      b="${f#paper-"$PAPER_VERSION"-}"; b="${b%.jar}"
      [[ "$b" =~ ^[0-9]+$ ]] && (( b > ${PAPER_BUILD:-0} )) && PAPER_BUILD="$b"
    done
    [[ -n "$PAPER_BUILD" ]] || { echo "Error: no stable build found for $PAPER_VERSION." >&2; exit 1; }
    echo "Could not resolve from API; using local build $PAPER_BUILD."
  fi
fi

EXPECTED_JAR="paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
if [[ -n "${MINECRAFT_SERVER_JAR:-}" && "$MINECRAFT_SERVER_JAR" != "$EXPECTED_JAR" ]]; then
  echo "Error: MINECRAFT_SERVER_JAR=$MINECRAFT_SERVER_JAR does not match PAPER_VERSION/PAPER_BUILD ($EXPECTED_JAR)." >&2
  exit 1
fi
MINECRAFT_SERVER_JAR="$EXPECTED_JAR"

if [[ ! -f "$MINECRAFT_SERVER_JAR" ]]; then
  echo "Downloading $MINECRAFT_SERVER_JAR..."
  BUILD_JSON=$(api_get "$FILL_API/builds/$PAPER_BUILD")
  DOWNLOAD_URL=$(jq -r '.downloads."server:default".url // empty' <<<"$BUILD_JSON")
  EXPECTED_SHA=$(jq -r '.downloads."server:default".checksums.sha256 // empty' <<<"$BUILD_JSON")
  [[ -n "$DOWNLOAD_URL" ]] || { echo "Error: no download for $PAPER_VERSION build $PAPER_BUILD." >&2; exit 1; }

  TMP_JAR="$MINECRAFT_SERVER_JAR.part"
  trap 'rm -f "$TMP_JAR"' EXIT
  curl -fL --progress-bar -H "User-Agent: $USER_AGENT" -o "$TMP_JAR" "$DOWNLOAD_URL"

  ACTUAL_SHA=$(shasum -a 256 "$TMP_JAR" | awk '{print $1}')
  if [[ -n "$EXPECTED_SHA" && "$ACTUAL_SHA" != "$EXPECTED_SHA" ]]; then
    echo "Error: checksum mismatch (expected $EXPECTED_SHA, got $ACTUAL_SHA)." >&2
    exit 1
  fi
  mv "$TMP_JAR" "$MINECRAFT_SERVER_JAR"
  trap - EXIT
fi

export MINECRAFT_SERVER_PATH MINECRAFT_SERVER_JAR

"$TOOLS_DIR/stop-server.sh"

if ! grep -qs '^eula=true' eula.txt; then
  echo "Note: accept the EULA by setting eula=true in $MINECRAFT_SERVER_PATH/eula.txt (server will exit until then)."
fi

echo "Starting $MINECRAFT_SERVER_JAR in $MINECRAFT_SERVER_PATH..."
# shellcheck disable=SC2086
exec java $JAVA_OPTS -jar "$MINECRAFT_SERVER_JAR" --nogui

