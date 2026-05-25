#!/usr/bin/env bash
set -e

echo "[verify-ui-language] Checking for suspicious English-only UI labels..."

TARGET_DIRS=("frontend/src" "src" "app" "pages" "components")

FOUND=0

for dir in "${TARGET_DIRS[@]}"; do
  if [ -d "$dir" ]; then
    if grep -RIn \
      --exclude-dir=node_modules \
      --exclude-dir=dist \
      --exclude-dir=build \
      --exclude='en.json' \
      -E '>(Confirm|Cancel|Submit|Save|Delete|Edit|Next|Back|Login|Sign Up|Search|Filter|Nearby|Ranking|Profile)<|"(Confirm|Cancel|Submit|Save|Delete|Edit|Next|Back|Login|Sign Up|Search|Filter|Nearby|Ranking|Profile)"' "$dir"; then
      FOUND=1
    fi
  fi
done

if [ "$FOUND" -eq 1 ]; then
  echo ""
  echo "[WARN] Suspicious English UI text detected."
  echo "Check whether user-facing labels should be Korean."
  exit 1
fi

echo "[verify-ui-language] No obvious English-only UI labels found."
