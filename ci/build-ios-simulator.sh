#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$gradle_home" build/ios-smoke
# Use the runner's JDK instead of the developer's Windows JDK path.
printf '\norg.gradle.java.home=%s\norg.gradle.workers.max=2\n' "$JAVA_HOME" >> "$gradle_home/gradle.properties"
chmod +x gradlew
python3 ci/validate-ios-plist.py iosApp/iosApp/Info.plist

xcodebuild -version
xcrun simctl list devices available
export KOTLIN_BUILD_LOG="$PWD/build/ios-smoke/kotlin-xcode.log"
xcodebuild -project iosApp/iosApp.xcodeproj -scheme KitapDuyUI \
  -configuration Release -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios-simulator \
  ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build-for-testing 2>&1 | tee build/ios-smoke/xcode-build.log
