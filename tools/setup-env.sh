#!/usr/bin/env bash
# Source (bash or zsh) to export the shared server config from ../.env
# (relative to this script):
#   PAPER_VERSION, PAPER_BUILD, MINECRAFT_SERVER_PATH, MINECRAFT_SERVER_JAR, MINECRAFT_BACKUP_PATH

_setup_env_self="${BASH_SOURCE[0]:-${(%):-%x}}"
_setup_env_root="$(cd "$(dirname "$_setup_env_self")/.." && pwd)"
_setup_env_file="${MINECRAFT_ENV_FILE:-$_setup_env_root/.env}"

if [[ -f "$_setup_env_file" ]]; then
  while IFS= read -r _line || [[ -n "$_line" ]]; do
    _line="${_line%$'\r'}"
    [[ "$_line" =~ ^[[:space:]]*(#|$) ]] && continue
    _key="${_line%%=*}"
    _value="${_line#*=}"
    _key="${_key//[[:space:]]/}"
    [[ "$_key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    _value="${_value#\"}"; _value="${_value%\"}"
    _value="${_value#\'}"; _value="${_value%\'}"
    eval "_current=\"\${$_key:-}\""
    [[ -z "$_current" ]] && export "$_key=$_value"
  done < "$_setup_env_file"
fi

PAPER_VERSION="${PAPER_VERSION:-}"
PAPER_BUILD="${PAPER_BUILD:-}"
MINECRAFT_SERVER_PATH="${MINECRAFT_SERVER_PATH:-MCServer}"
MINECRAFT_SERVER_PATH="${MINECRAFT_SERVER_PATH/#\~/$HOME}"
[[ "$MINECRAFT_SERVER_PATH" == /* ]] || MINECRAFT_SERVER_PATH="$_setup_env_root/$MINECRAFT_SERVER_PATH"
MINECRAFT_BACKUP_PATH="${MINECRAFT_BACKUP_PATH:-backups}"
MINECRAFT_BACKUP_PATH="${MINECRAFT_BACKUP_PATH/#\~/$HOME}"
[[ "$MINECRAFT_BACKUP_PATH" == /* ]] || MINECRAFT_BACKUP_PATH="$_setup_env_root/$MINECRAFT_BACKUP_PATH"

if [[ -z "${MINECRAFT_SERVER_JAR:-}" && -n "$PAPER_VERSION" && -n "$PAPER_BUILD" ]]; then
  MINECRAFT_SERVER_JAR="paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
fi

export PAPER_VERSION PAPER_BUILD MINECRAFT_SERVER_PATH MINECRAFT_SERVER_JAR MINECRAFT_BACKUP_PATH
unset _setup_env_self _setup_env_root _setup_env_file _line _key _value _current
