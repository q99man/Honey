#!/usr/bin/env bash
set -e

echo "[format-check]"
if [ -x ./gradlew ]; then
  ./gradlew spotlessCheck
elif [ -x ./backend/gradlew ]; then
  (cd backend && ./gradlew spotlessCheck)
elif [ -f ./frontend/package.json ]; then
  (cd frontend && npm run lint)
else
  echo "[format-check] No formatter target found. Skipping."
fi
