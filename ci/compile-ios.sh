#!/usr/bin/env bash
# Keep the complete compiler output and propagate failures through tee.
set -euo pipefail
cd "$(dirname "$0")/.."
log_path="${KOTLIN_BUILD_LOG:-/tmp/kotlin-xcode.log}"

# Ensure sufficient heap memory (6GB) and G1GC for Gradle daemon and Kotlin/Native linking
current_opts="${GRADLE_OPTS:-}"
if [[ "$current_opts" != *"-Xmx6g"* && "$current_opts" != *"-Xmx6G"* && "$current_opts" != *"-Xmx8g"* && "$current_opts" != *"-Xmx8G"* ]]; then
  export GRADLE_OPTS="${current_opts:+$current_opts }-Dorg.gradle.jvmargs=-Xmx6g -XX:+UseG1GC -Dkotlin.native.jvmArgs=-Xmx6g"
fi

if ./gradlew "$@" --no-daemon --console=plain --stacktrace 2>&1 | tee "$log_path"; then
  exit 0
else
  status=$?
  printf '\nKotlin build failed (exit %s). Full log: %s\n' "$status" "$log_path"
  grep -nE '(^e:|error:|What went wrong:|Execution failed)' "$log_path" || true
  exit "$status"
fi
