#!/usr/bin/env bash
# Helpers for changelog.md, the source of truth for release notes.
#
# changelog.md starts with a first-level header naming the release it describes:
#   # Changelog for v1.2.3
# followed by Markdown release notes. HTML comments are ignored.
#
# Usage:
#   tools/changelog.sh get-version         print the version named in the header
#   tools/changelog.sh notes               print the release notes (no header);
#                                          fails if there are none
# CHANGELOG_FILE overrides the file (default: <project root>/changelog.md).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CHANGELOG_FILE="${CHANGELOG_FILE:-$PROJECT_ROOT/changelog.md}"

die() { echo "changelog: $*" >&2; exit 1; }
[[ -f "$CHANGELOG_FILE" ]] || die "$CHANGELOG_FILE does not exist. Create it with a '# Changelog for v<version>' header."

# Prints the first first-level header line, if any.
header_line() { awk '/^# / { print; exit }' "$CHANGELOG_FILE"; }

get_version() {
  local header version
  header="$(header_line)"
  [[ -n "$header" ]] || die "$CHANGELOG_FILE has no first-level header ('# Changelog for v<version>')."
  version="$(printf '%s\n' "$header" | sed -n -E 's/^# +[Cc]hangelog for +v?([^ ]+) *$/\1/p')"
  [[ -n "$version" ]] || die "header '$header' does not look like '# Changelog for v<version>'."
  printf '%s\n' "$version"
}

notes() {
  local out
  out="$(awk '
    function flush_blank() { for (; pending > 0; pending--) print "" }
    BEGIN { seen_header = 0; incomment = 0; started = 0; pending = 0 }
    !seen_header { if ($0 ~ /^# /) seen_header = 1; next }
    {
      line = $0; text = ""
      while (length(line) > 0) {
        if (incomment) {
          i = index(line, "-->")
          if (i == 0) line = ""; else { line = substr(line, i + 3); incomment = 0 }
        } else {
          i = index(line, "<!--")
          if (i == 0) { text = text line; line = "" }
          else { text = text substr(line, 1, i - 1); line = substr(line, i + 4); incomment = 1 }
        }
      }
      if (text ~ /^[ \t\r]*$/) {
        if ($0 ~ /^[ \t\r]*$/ && started) pending++     # genuine blank line
        next                                            # comment-only line: drop
      }
      if (started) flush_blank()
      pending = 0; started = 1
      print text
    }' "$CHANGELOG_FILE")"
  [[ -n "$out" ]] || die "$CHANGELOG_FILE has no release notes under its header. Describe what changed in this release (HTML comments do not count)."
  printf '%s\n' "$out"
}

case "${1:-}" in
  get-version) get_version ;;
  notes) notes ;;
  -h|--help|"") sed -n '2,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; [[ -n "${1:-}" ]] ;;
  *) die "unknown command '$1' (try --help)" ;;
esac
