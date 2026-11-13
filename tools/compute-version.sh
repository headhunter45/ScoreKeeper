#!/usr/bin/env bash
# Prints the project version derived from Git tags (without the leading "v").
# Gradle, CI and the release workflow all call this so they agree on the version.
#
# Rules (tags look like v1.2.3 or v1.2.3-suffix):
#   1. If the commit has tags, use the greatest release tag (no suffix). If it
#      only has suffixed tags, use the greatest of those.
#   2. Otherwise look at all tags reachable from the commit and take the greatest
#      version. If that tag ends in "-dev", use it as is. Otherwise bump the patch
#      number by one and append "-dev" (v1.2.3 -> 1.2.4-dev).
#   3. With no tags at all the version is 0.0.1-dev.
#
# Usage: tools/compute-version.sh [--explain] [rev]     (rev defaults to HEAD)
set -euo pipefail

EXPLAIN=0
REV="HEAD"
for arg in "$@"; do
  case "$arg" in
    --explain) EXPLAIN=1 ;;
    -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) REV="$arg" ;;
  esac
done

explain() {
  if [[ "$EXPLAIN" == 1 ]]; then echo "compute-version: $*" >&2; fi
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "compute-version: not a Git checkout; using 0.0.1-dev" >&2
  echo "0.0.1-dev"
  exit 0
fi
if ! git rev-parse --verify -q "${REV}^{commit}" >/dev/null; then
  echo "compute-version: '$REV' is not a commit in this repository." >&2
  exit 1
fi
if [[ "$(git rev-parse --is-shallow-repository 2>/dev/null || echo false)" == "true" ]]; then
  echo "compute-version: warning: shallow clone, tags may be missing (use fetch-depth: 0)." >&2
fi

# Reads tag names on stdin and prints the best v<major>.<minor>.<patch>[-suffix] one.
# mode "head": a release tag beats any suffixed tag, then the higher number wins.
# mode "ancestor": the higher number wins, then a release tag beats a suffixed one.
pick_best() {
  awk -v mode="$1" '
    /^v[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$/ {
      core = substr($0, 2); suffix = ""
      i = index(core, "-")
      if (i > 0) { suffix = substr(core, i + 1); core = substr(core, 1, i - 1) }
      split(core, p, ".")
      flag = (suffix == "") ? "1" : "0"
      num = sprintf("%09d.%09d.%09d", p[1], p[2], p[3])
      key = (mode == "head") ? flag "|" num "|" suffix : num "|" flag "|" suffix
      if (best == "" || key > best) { best = key; besttag = $0 }
    }
    END { if (besttag != "") print besttag }'
}

best="$(git tag --points-at "$REV" | pick_best head)"
if [[ -n "$best" ]]; then
  explain "$REV is tagged; using $best"
  echo "${best#v}"
  exit 0
fi

best="$(git tag --merged "$REV" | pick_best ancestor)"
if [[ -z "$best" ]]; then
  explain "no v* tags reachable from $REV"
  echo "0.0.1-dev"
  exit 0
fi

core="${best#v}"
numeric="${core%%-*}"
suffix=""
if [[ "$core" == *-* ]]; then suffix="${core#*-}"; fi
if [[ "$suffix" == "dev" ]]; then
  explain "$REV is untagged; greatest ancestor tag is $best (already -dev)"
  echo "$core"
  exit 0
fi

IFS=. read -r major minor patch <<<"$numeric"
explain "$REV is untagged; greatest ancestor tag is $best; bumping patch and adding -dev"
echo "$major.$minor.$((patch + 1))-dev"
