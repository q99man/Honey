#!/usr/bin/env bash
set -e

echo "[format-check]"

cd "$(dirname "$0")/../backend"

./gradlew clean build -x test