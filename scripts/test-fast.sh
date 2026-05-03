#!/usr/bin/env bash
set -e

echo "[test]"
if [ -f ./backend/gradlew ] || [ -f ./backend/gradlew.bat ]; then
  ./scripts/run-backend-gradle.sh test
elif [ -f ./gradlew ]; then
  ./gradlew test
elif [ -f ./frontend/package.json ]; then
  (cd frontend && npm run build)
else
  echo "[test] No test target found. Skipping."
fi
