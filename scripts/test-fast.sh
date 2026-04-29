#!/usr/bin/env bash
set -e

echo "[test]"
if [ -x ./gradlew ]; then
  ./gradlew test
elif [ -x ./backend/gradlew ]; then
  (cd backend && ./gradlew test)
elif [ -f ./frontend/package.json ]; then
  (cd frontend && npm run build)
else
  echo "[test] No test target found. Skipping."
fi
