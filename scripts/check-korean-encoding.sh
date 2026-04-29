#!/usr/bin/env bash
set -e

echo "[check-korean-encoding] Scanning for possible mojibake..."

EXCLUDES=(
  --exclude-dir=.git
  --exclude-dir=build
  --exclude-dir=out
  --exclude-dir=dist
  --exclude-dir=node_modules
  --exclude-dir=.idea
  --exclude-dir=.gradle
)

PATTERN='�|Ã|ì|ë|ì•|ìž|í•|ìœ|ë‹|ì–|ì„'
FILES=$(find . \
  -path './.git' -prune -o \
  -path './build' -prune -o \
  -path './out' -prune -o \
  -path './dist' -prune -o \
  -path './node_modules' -prune -o \
  -path './frontend/node_modules' -prune -o \
  -path './.idea' -prune -o \
  -path './.gradle' -prune -o \
  -path './scripts/check-korean-encoding.sh' -prune -o \
  -path './docs/ui-language.md' -prune -o \
  -type f -print)

if [ -n "$FILES" ] && grep -In -E "$PATTERN" $FILES; then
  echo ""
  echo "[ERROR] Possible broken Korean text detected."
  echo "Fix encoding before commit."
  exit 1
fi

echo "[check-korean-encoding] No obvious mojibake patterns found."
