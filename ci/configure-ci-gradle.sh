#!/usr/bin/env bash
# Configure GRADLE_USER_HOME/gradle.properties for CI runners (including 7 GB macos-26).
set -euo pipefail

gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
props_file="$gradle_home/gradle.properties"
mkdir -p "$gradle_home"

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

tmp_file="$(mktemp)"
trap 'rm -f "$tmp_file"' EXIT
if [[ -f "$props_file" ]]; then
  awk '
    /^# BEGIN KITAPDUY CI GRADLE$/ { skip=1; next }
    /^# END KITAPDUY CI GRADLE$/ { skip=0; next }
    !skip { print }
  ' "$props_file" > "$tmp_file"
fi

{
  printf '\n# BEGIN KITAPDUY CI GRADLE\n'
  if [[ -n "${JAVA_HOME:-}" ]]; then
    # Use the runner JDK instead of the developer Windows JDK path in repo gradle.properties.
    java_home="${JAVA_HOME//\\//}"
    printf 'org.gradle.java.home=%s\n' "$java_home"
  fi
  if [[ "${mem_bytes:-0}" -gt 0 && "${mem_bytes:-0}" -le 8589934592 ]]; then
    # 7-8 GB runners (e.g. GitHub macos-26): cap JVM heaps and serialize workers to prevent OOM/swap thrashing.
    printf 'org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseG1GC\n'
    printf 'kotlin.daemon.jvmargs=-Xmx2048m -XX:+UseG1GC\n'
    printf 'kotlin.native.jvmArgs=-Xmx4096m -XX:+UseG1GC\n'
    printf 'org.gradle.parallel=false\n'
    printf 'org.gradle.tooling.parallel=false\n'
    printf 'org.gradle.workers.max=1\n'
  else
    printf 'org.gradle.workers.max=2\n'
  fi
  printf '# END KITAPDUY CI GRADLE\n'
} >> "$tmp_file"

mv "$tmp_file" "$props_file"
