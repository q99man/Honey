#!/usr/bin/env bash
set -e

SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
SCRIPT_DIR="${SCRIPT_PATH%/*}"
if [ "$SCRIPT_DIR" = "$SCRIPT_PATH" ]; then
  SCRIPT_DIR="."
fi
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

cd "$BACKEND_DIR"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$BACKEND_DIR/.gradle-user-home}"

ensure_java_home() {
  if [ -n "${JAVA_HOME:-}" ] || command -v java >/dev/null 2>&1; then
    return
  fi

  local candidates=()
  if [ -n "${HOME:-}" ]; then
    candidates+=("$HOME/.jdks/ms-21.0.10")
    candidates+=("$HOME/.jdks/ms-21"*)
    candidates+=("$HOME/.jdks/openjdk-21"*)
  fi
  candidates+=("/c/Users/Seo/.jdks/ms-21.0.10")
  candidates+=("/c/Users/Seo/.jdks/ms-21"*)
  candidates+=("/c/Users/Seo/.jdks/openjdk-21"*)

  local candidate
  for candidate in "${candidates[@]}"; do
    if [ -x "$candidate/bin/java" ] || [ -x "$candidate/bin/java.exe" ]; then
      if command -v cygpath >/dev/null 2>&1; then
        export JAVA_HOME="$(cygpath -w "$candidate")"
      else
        case "$candidate" in
          /[a-zA-Z]/*)
            local drive="${candidate:1:1}"
            local rest="${candidate:3}"
            drive="$(printf '%s' "$drive" | tr '[:lower:]' '[:upper:]')"
            rest="${rest//\//\\}"
            export JAVA_HOME="${drive}:\\${rest}"
            ;;
          *)
            export JAVA_HOME="$candidate"
            ;;
        esac
      fi
      export PATH="$candidate/bin:$PATH"
      return
    fi
  done
}

ensure_java_home

IS_WINDOWS=false
if [ "${OS:-}" = "Windows_NT" ] || [ -n "${WINDIR:-}" ]; then
  IS_WINDOWS=true
else
  case "$(uname -s 2>/dev/null || echo unknown)" in
    CYGWIN*|MINGW*|MSYS*)
      IS_WINDOWS=true
      ;;
  esac
fi

if [ "$IS_WINDOWS" = "true" ] && [ -f ./gradlew.bat ]; then
  ./gradlew.bat "$@"
  exit $?
fi

if [ -x ./gradlew ]; then
  ./gradlew "$@"
elif [ -f ./gradlew ]; then
  sh ./gradlew "$@"
else
  echo "[gradle] backend Gradle wrapper not found."
  exit 1
fi
