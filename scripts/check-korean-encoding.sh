#!/usr/bin/env bash
set -e

echo "[check-korean-encoding] Scanning for possible mojibake..."

PATTERN='占|�|Ã|Â|챙|챘'

if find . \
  -path '*/.git' -prune -o \
  -path '*/build' -prune -o \
  -path '*/out' -prune -o \
  -path '*/dist' -prune -o \
  -path '*/node_modules' -prune -o \
  -path '*/.idea' -prune -o \
  -path '*/.gradle' -prune -o \
  -path '*/.gradle-user-home' -prune -o \
  -path './scripts/check-korean-encoding.sh' -prune -o \
  -path './docs/ui-language.md' -prune -o \
  -type f -print0 | xargs -0 grep -IIn -E "$PATTERN"; then
  echo ""
  echo "[ERROR] Possible broken Korean text detected."
  echo "Fix encoding before commit."
  exit 1
fi

echo "[check-korean-encoding] No obvious mojibake patterns found."
