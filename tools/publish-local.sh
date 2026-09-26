#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_ROOT/.gradle/user-home}"
mkdir -p "$GRADLE_USER_HOME/jdks"

ensure_java_25() {
  local version="25"
  local os arch archive java_bin actual_home

  case "$(uname -s)" in
    Darwin) os="mac" ;;
    Linux) os="linux" ;;
    *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
  esac

  case "$(uname -m)" in
    arm64|aarch64) arch="aarch64" ;;
    x86_64|amd64) arch="x64" ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
  esac

  java_bin=$(find "$GRADLE_USER_HOME/jdks" -path '*/bin/java' -type f 2>/dev/null | head -n 1 || true)
  if [[ -z "$java_bin" ]]; then
    echo "Downloading Temurin JDK $version into $GRADLE_USER_HOME/jdks..."
    archive="$GRADLE_USER_HOME/jdks/temurin-$version.tar.gz"
    curl -fsSL "https://api.adoptium.net/v3/binary/latest/$version/ga/${os}/${arch}/jdk/hotspot/normal/eclipse" -o "$archive"
    tar -xzf "$archive" -C "$GRADLE_USER_HOME/jdks"
    rm -f "$archive"
    java_bin=$(find "$GRADLE_USER_HOME/jdks" -path '*/bin/java' -type f 2>/dev/null | head -n 1 || true)
  fi

  [[ -n "$java_bin" ]] || { echo "Error: could not locate installed JDK 25." >&2; exit 1; }
  actual_home="$(dirname "$(dirname "$java_bin")")"
  export JAVA_HOME="$actual_home"
  export PATH="$JAVA_HOME/bin:$PATH"
}

ensure_java_25
cd "$PROJECT_ROOT"
./gradlew publishToMavenLocal "$@"
