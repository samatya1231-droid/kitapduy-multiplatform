#!/usr/bin/env bash
# Keep the complete compiler output and propagate failures through tee.
set -euo pipefail
cd "$(dirname "$0")/.."
log_path="${KOTLIN_BUILD_LOG:-/tmp/kotlin-xcode.log}"
if ./gradlew "$@" --no-daemon --console=plain --stacktrace 2>&1 | tee "$log_path"; then
  exit 0
else
  status=$?
  printf '\nKotlin build failed (exit %s). Full log: %s\n' "$status" "$log_path"
  grep -nE '(^e:|error:|What went wrong:|Execution failed)' "$log_path" || true
  exit "$status"
fi
