#!/usr/bin/env bash
set -e

echo "[format-check]"

SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
SCRIPT_DIR="${SCRIPT_PATH%/*}"
if [ "$SCRIPT_DIR" = "$SCRIPT_PATH" ]; then
  SCRIPT_DIR="."
fi

"${BASH:-bash}" "$SCRIPT_DIR/run-backend-gradle.sh" clean build -x test
