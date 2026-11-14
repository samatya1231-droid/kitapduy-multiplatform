#!/usr/bin/env bash
# Keep the complete compiler output and propagate failures through tee.
set -euo pipefail
cd "$(dirname "$0")/.."
log_path="${KOTLIN_BUILD_LOG:-/tmp/kotlin-xcode.log}"
mkdir -p "$(dirname "$log_path")"

# Ensure sufficient heap memory and G1GC for Gradle daemon and Kotlin/Native linking
current_opts="${GRADLE_OPTS:-}"
extra_gradle_args=()
if [[ "$current_opts" != *"-Xmx"* ]]; then
  mem_bytes="${KITAPDUY_CI_MEM_BYTES:-}"
  if [[ -z "$mem_bytes" ]]; then
    if command -v sysctl >/dev/null 2>&1; then
      mem_bytes="$(sysctl -n hw.memsize 2>/dev/null || echo 0)"
    elif [[ -r /proc/meminfo ]]; then
      mem_kb="$(awk '/^MemTotal:/ {print $2; exit}' /proc/meminfo 2>/dev/null || echo 0)"
      mem_bytes=$(( ${mem_kb:-0} * 1024 ))
    else
      mem_bytes=0
    fi
  fi
  if [[ "${mem_bytes:-0}" -gt 0 && "${mem_bytes:-0}" -le 8589934592 ]]; then
    export GRADLE_OPTS="${current_opts:+$current_opts }\"-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8 -XX:+UseG1GC\""
    extra_gradle_args=(
      --no-parallel
      --max-workers=1
      "-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8 -XX:+UseG1GC"
      "-Pkotlin.daemon.jvmargs=-Xmx1536m -XX:+UseG1GC"
      "-Pkotlin.native.jvmArgs=-Xmx3584m -XX:+UseG1GC"
    )
  else
    export GRADLE_OPTS="${current_opts:+$current_opts }\"-Dorg.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8 -XX:+UseG1GC\""
    extra_gradle_args=(
      "-Dorg.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8 -XX:+UseG1GC"
      "-Pkotlin.native.jvmArgs=-Xmx6g"
    )
  fi
fi

if ./gradlew "$@" ${extra_gradle_args[@]+"${extra_gradle_args[@]}"} --no-daemon --console=plain --stacktrace 2>&1 | tee "$log_path"; then
  exit 0
else
  status=$?
  printf '\nKotlin build failed (exit %s). Full log: %s\n' "$status" "$log_path"
  grep -nE '(^e:|error:|What went wrong:|Execution failed)' "$log_path" || true
  exit "$status"
fi
