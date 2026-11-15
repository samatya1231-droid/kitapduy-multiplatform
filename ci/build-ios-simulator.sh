#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p build/ios-smoke
bash ci/configure-ci-gradle.sh
chmod +x gradlew
python3 ci/validate-ios-plist.py iosApp/iosApp/Info.plist --localization tr --exact-localizations

xcodebuild -version
xcrun simctl list devices available
export KOTLIN_BUILD_LOG="$PWD/build/ios-smoke/kotlin-xcode.log"
xcodebuild -project iosApp/iosApp.xcodeproj -scheme KitapDuyUI \
  -configuration Release -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios-simulator \
  -clonedSourcePackagesDirPath "${SPM_CACHE_DIR:-$PWD/.spm-cache}" \
  ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build-for-testing 2>&1 | tee build/ios-smoke/xcode-build.log

python3 ci/validate-ios-plist.py build/ios-simulator/Build/Products/Release-iphonesimulator/KitapDuy.app \
  --bundle-id com.semtinazizi.kitapduy \
  --development-region tr --localization tr --exact-localizations

# Release JVM daemon memory before booting iPhone/iPad simulators on 7 GB runners.
./gradlew --stop >/dev/null 2>&1 || true
pkill -f '[K]otlinCompileDaemon' >/dev/null 2>&1 || true
