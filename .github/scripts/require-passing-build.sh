#!/usr/bin/env bash
# Fails unless the CI check run CHECK_NAME finished successfully on commit SHA.
# Used by release.yml so a release never builds a commit that CI has not passed.
#
# Required env: GH_TOKEN, REPOSITORY (owner/name), SHA, CHECK_NAME
# Optional env: LABEL (what is being released, used in messages)
# GH_TOKEN needs "checks: read".
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}" "${REPOSITORY:?REPOSITORY is required}"
: "${SHA:?SHA is required}" "${CHECK_NAME:?CHECK_NAME is required}"
LABEL="${LABEL:-$SHA}"
SHORT_SHA="${SHA:0:12}"

refuse() {
  local reason="$1"
  echo "::error title=Refusing to release ${LABEL}::${reason//$'\n'/%0A}"
  printf 'Refusing to release %s:\n%s\n' "$LABEL" "$reason" >&2
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    printf '### Release refused: %s\n\n%s\n' "$LABEL" "$reason" >> "$GITHUB_STEP_SUMMARY"
  fi
  exit 1
}

runs="$(gh api --paginate "repos/${REPOSITORY}/commits/${SHA}/check-runs?filter=latest&per_page=100" \
  --jq '.check_runs[]' \
  | jq -cs --arg name "$CHECK_NAME" '[.[] | select(.name == $name and .app.slug == "github-actions")]')"

if [[ "$(jq 'length' <<<"$runs")" == 0 ]]; then
  refuse "No '${CHECK_NAME}' check run exists for commit ${SHORT_SHA}, so this commit has not been built by CI.
Push the commit to main or develop (or open a pull request for it) and wait for the 'Java CI with Gradle' workflow to pass, then run this release again."
fi

running="$(jq -r '.[] | select(.status != "completed") | "- \(.name) is \(.status): \(.html_url)"' <<<"$runs")"
if [[ -n "$running" ]]; then
  refuse "The CI build for commit ${SHORT_SHA} has not finished yet:
${running}
Wait for it to pass, then run this release again."
fi

failed="$(jq -r '.[] | select(.conclusion != "success") | "- \(.name) finished with conclusion \"\(.conclusion)\": \(.html_url)"' <<<"$runs")"
if [[ -n "$failed" ]]; then
  refuse "The CI build for commit ${SHORT_SHA} did not pass:
${failed}
Fix the failure (the linked run shows which step failed), get CI green on a commit, tag that commit, and run this release again."
fi

echo "CI check '${CHECK_NAME}' passed for commit ${SHORT_SHA}:"
jq -r '.[] | "- \(.html_url)"' <<<"$runs"
