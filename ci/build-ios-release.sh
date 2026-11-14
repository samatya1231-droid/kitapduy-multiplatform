#!/usr/bin/env bash
# Build, sign, validate, and optionally upload the iOS App Store IPA from CI.
set -euo pipefail
cd "$(dirname "$0")/.."

BUNDLE_ID="${BUNDLE_ID:-com.semtinazizi.kitapduy}"
export UPLOAD_TO_APP_STORE="${UPLOAD_TO_APP_STORE:-false}"
RUN_NUMBER="${GITHUB_RUN_NUMBER:-1}"
BUILD_NUMBER_INPUT="${BUILD_NUMBER_INPUT:-}"
MARKETING_VERSION_INPUT="${MARKETING_VERSION_INPUT:-}"

WORK_DIR="$PWD/build/ios-release"
mkdir -p "$WORK_DIR/ipa"

SIGN_DIR="$(mktemp -d "${RUNNER_TEMP:-/tmp}/kitapduy-signing.XXXXXX")"
chmod 700 "$SIGN_DIR"
KEYCHAIN_PATH="$SIGN_DIR/build.keychain-db"
KEYCHAIN_PASS="$(openssl rand -hex 24)"
ORIG_DEFAULT_KEYCHAIN="$(security default-keychain -d user 2>/dev/null | tr -d '"' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' || true)"
ORIG_KEYCHAINS=()
while IFS= read -r line; do
  clean="${line//\"/}"
  clean="${clean#"${clean%%[![:space:]]*}"}"
  clean="${clean%"${clean##*[![:space:]]}"}"
  [[ -n "$clean" ]] && ORIG_KEYCHAINS+=("$clean")
done < <(security list-keychains -d user)

cleanup() {
  set +e
  if [[ -f "$KEYCHAIN_PATH" ]]; then
    security delete-keychain "$KEYCHAIN_PATH" >/dev/null 2>&1 || true
  fi
  if [[ ${#ORIG_KEYCHAINS[@]} -gt 0 ]]; then
    security list-keychains -d user -s "${ORIG_KEYCHAINS[@]}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${ORIG_DEFAULT_KEYCHAIN:-}" ]]; then
    security default-keychain -d user -s "$ORIG_DEFAULT_KEYCHAIN" >/dev/null 2>&1 || true
  fi
  if [[ -n "${INSTALLED_PROFILE:-}" ]]; then
    rm -f "$INSTALLED_PROFILE"
  fi
  rm -rf "$SIGN_DIR"
}
trap cleanup EXIT

# 1. Decode signing secrets into restricted temp storage.
python3 ci/ios_release.py decode-secrets --out-dir "$SIGN_DIR"
unset IOS_BUILD_CERTIFICATE_BASE64 IOS_PROVISION_PROFILE_BASE64 APP_STORE_CONNECT_API_KEY_BASE64

# 2. Fail fast if the provisioning profile is missing capabilities, wrong type, or expired.
security cms -D -i "$SIGN_DIR/profile.mobileprovision" > "$SIGN_DIR/profile.plist"
python3 ci/ios_release.py validate-profile \
  --profile-plist "$SIGN_DIR/profile.plist" \
  --bundle-id "$BUNDLE_ID" \
  --out-json "$SIGN_DIR/profile.json"

PROFILE_UUID="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["uuid"])' "$SIGN_DIR/profile.json")"
PROFILE_NAME="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["name"])' "$SIGN_DIR/profile.json")"
TEAM_ID="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["team_id"])' "$SIGN_DIR/profile.json")"
echo "Verified App Store profile: $PROFILE_NAME ($PROFILE_UUID), Team: $TEAM_ID"

# 3. Create ephemeral keychain and import .p12 (with OpenSSL 3 modern -> legacy fallback).
security create-keychain -p "$KEYCHAIN_PASS" "$KEYCHAIN_PATH"
security set-keychain-settings -lut 21600 "$KEYCHAIN_PATH"
security unlock-keychain -p "$KEYCHAIN_PASS" "$KEYCHAIN_PATH"
if [[ ${#ORIG_KEYCHAINS[@]} -gt 0 ]]; then
  security list-keychains -d user -s "$KEYCHAIN_PATH" "${ORIG_KEYCHAINS[@]}"
else
  security list-keychains -d user -s "$KEYCHAIN_PATH"
fi
security default-keychain -d user -s "$KEYCHAIN_PATH"

import_p12() {
  security import "$1" -k "$KEYCHAIN_PATH" -P "$IOS_P12_PASSWORD" \
    -T /usr/bin/codesign -T /usr/bin/security >/dev/null 2>&1
}

if ! import_p12 "$SIGN_DIR/distribution.p12"; then
  echo "Standard security import failed; attempting OpenSSL legacy PKCS#12 conversion..."
  ossl_bin=""
  for candidate in \
    /opt/homebrew/opt/openssl@3/bin/openssl \
    /opt/homebrew/opt/openssl@4/bin/openssl \
    /opt/homebrew/bin/openssl \
    /usr/local/opt/openssl@3/bin/openssl \
    /usr/local/bin/openssl \
    "$(command -v openssl 2>/dev/null || true)"; do
    if [[ -n "$candidate" && -x "$candidate" ]] && "$candidate" pkcs12 -help 2>&1 | grep -q -- '-legacy'; then
      ossl_bin="$candidate"
      break
    fi
  done
  if [[ -z "$ossl_bin" ]]; then
    echo "ERROR: security import failed and no OpenSSL with PKCS#12 -legacy support was found (check IOS_P12_PASSWORD)." >&2
    exit 1
  fi
  printf '%s' "$IOS_P12_PASSWORD" > "$SIGN_DIR/p12pass"
  chmod 600 "$SIGN_DIR/p12pass"
  if ! "$ossl_bin" pkcs12 -in "$SIGN_DIR/distribution.p12" -nodes -out "$SIGN_DIR/temp.pem" -passin "file:$SIGN_DIR/p12pass" 2>/dev/null; then
    if ! "$ossl_bin" pkcs12 -legacy -in "$SIGN_DIR/distribution.p12" -nodes -out "$SIGN_DIR/temp.pem" -passin "file:$SIGN_DIR/p12pass"; then
      rm -f "$SIGN_DIR/temp.pem" "$SIGN_DIR/p12pass"
      echo "ERROR: Failed to decrypt distribution.p12 (verify IOS_P12_PASSWORD and IOS_BUILD_CERTIFICATE_BASE64)." >&2
      exit 1
    fi
  fi
  "$ossl_bin" pkcs12 -export -legacy -in "$SIGN_DIR/temp.pem" -out "$SIGN_DIR/distribution-legacy.p12" -passout "file:$SIGN_DIR/p12pass"
  rm -f "$SIGN_DIR/temp.pem" "$SIGN_DIR/p12pass"
  security import "$SIGN_DIR/distribution-legacy.p12" -k "$KEYCHAIN_PATH" -P "$IOS_P12_PASSWORD" \
    -T /usr/bin/codesign -T /usr/bin/security >/dev/null
fi
unset IOS_P12_PASSWORD

security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$KEYCHAIN_PASS" "$KEYCHAIN_PATH" >/dev/null

# Ensure Apple WWDR intermediate certificates are present if codesigning identity trust is incomplete.
if ! security find-identity -v -p codesigning "$KEYCHAIN_PATH" | grep -qE '[0-9A-F]{40}'; then
  echo "Downloading Apple WWDR intermediate certificates for trust verification..."
  for wwdr_name in AppleWWDRCAG3.cer AppleWWDRCAG4.cer AppleWWDRCAG6.cer; do
    if curl -fsSL -o "$SIGN_DIR/$wwdr_name" "https://www.apple.com/certificateauthority/$wwdr_name"; then
      security import "$SIGN_DIR/$wwdr_name" -k "$KEYCHAIN_PATH" -T /usr/bin/codesign >/dev/null || true
    fi
  done
fi

INSTALLED_SHA256S=()
while IFS= read -r sha; do
  [[ -n "$sha" ]] && INSTALLED_SHA256S+=("$sha")
done < <(
  security find-certificate -a -Z "$KEYCHAIN_PATH" \
    | awk '/^SHA-256 hash:/ {print $3}'
)
if [[ ${#INSTALLED_SHA256S[@]} -eq 0 ]]; then
  echo "ERROR: No certificates found inside imported .p12." >&2
  exit 1
fi

VALID_IDENTITY_SHA1S=()
while IFS= read -r sha1; do
  [[ -n "$sha1" ]] && VALID_IDENTITY_SHA1S+=("$sha1")
done < <(
  security find-identity -v -p codesigning "$KEYCHAIN_PATH" \
    | awk '/^[[:space:]]*[0-9]+\)/ && length($2) == 40 && $2 ~ /^[0-9A-Fa-f]+$/ {print $2}'
)

match_args=(--profile-json "$SIGN_DIR/profile.json" --cert-sha256 "${INSTALLED_SHA256S[@]}")
if [[ ${#VALID_IDENTITY_SHA1S[@]} -gt 0 ]]; then
  match_args+=(--identity-sha1 "${VALID_IDENTITY_SHA1S[@]}")
fi
SIGN_SHA1="$(python3 ci/ios_release.py match-cert "${match_args[@]}")"

if ! security find-identity -v -p codesigning "$KEYCHAIN_PATH" | grep -qi "$SIGN_SHA1"; then
  echo "ERROR: Certificate $SIGN_SHA1 matched the profile, but has no valid codesigning private key/trust in keychain." >&2
  security find-identity -p codesigning "$KEYCHAIN_PATH" >&2 || true
  exit 1
fi
echo "Verified codesigning identity SHA-1: $SIGN_SHA1"

# Xcode 26 reads profiles from the Xcode user data directory.
profile_dir="$HOME/Library/Developer/Xcode/UserData/Provisioning Profiles"
mkdir -p "$profile_dir"
profile_path="$profile_dir/$PROFILE_UUID.mobileprovision"
if [[ -e "$profile_path" ]]; then
  cmp "$SIGN_DIR/profile.mobileprovision" "$profile_path"
else
  cp "$SIGN_DIR/profile.mobileprovision" "$profile_path"
  INSTALLED_PROFILE="$profile_path"
fi

# 5. Bump version/build number in Info.plist (CI workspace only) and generate ExportOptions.plist.
prep_output="$(python3 ci/ios_release.py prepare-build \
  --info-plist iosApp/iosApp/Info.plist \
  --run-number "$RUN_NUMBER" \
  --build-number "$BUILD_NUMBER_INPUT" \
  --marketing-version "$MARKETING_VERSION_INPUT" \
  --export-options "$WORK_DIR/ExportOptions.plist" \
  --profile-json "$SIGN_DIR/profile.json" \
  --signing-cert "$SIGN_SHA1")"
eval "$prep_output"
echo "Release version: $SHORT_VERSION ($BUILD_VERSION)"

python3 ci/validate-ios-plist.py iosApp/iosApp/Info.plist

# 6. Configure Gradle for 7 GB macos-26 runner and build/archive Release .xcarchive.
bash ci/configure-ci-gradle.sh
chmod +x gradlew
xcodebuild -version

export KOTLIN_BUILD_LOG="$WORK_DIR/kotlin-xcode.log"
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath "$WORK_DIR/derived-data" \
  -clonedSourcePackagesDirPath "${SPM_CACHE_DIR:-$PWD/.spm-cache}" \
  -archivePath "$WORK_DIR/KitapDuy.xcarchive" \
  BUNDLE_ID="$BUNDLE_ID" \
  TEAM_ID="$TEAM_ID" \
  APP_CODE_SIGN_STYLE=Manual \
  APP_CODE_SIGN_IDENTITY="$SIGN_SHA1" \
  APP_PROVISIONING_PROFILE_SPECIFIER="$PROFILE_NAME" \
  archive 2>&1 | tee "$WORK_DIR/xcode-archive.log"

# Free JVM daemon memory and temporary DerivedData disk space before exporting IPA / uploading.
./gradlew --stop >/dev/null 2>&1 || true
pkill -f '[K]otlinCompileDaemon' >/dev/null 2>&1 || true
rm -rf "$WORK_DIR/derived-data"

# 7. Export signed App Store IPA and validate its Info.plist.
xcodebuild -exportArchive \
  -archivePath "$WORK_DIR/KitapDuy.xcarchive" \
  -exportPath "$WORK_DIR/ipa" \
  -exportOptionsPlist "$WORK_DIR/ExportOptions.plist" 2>&1 | tee "$WORK_DIR/xcode-export.log"

shopt -s nullglob
ipas=("$WORK_DIR"/ipa/*.ipa)
if [[ ${#ipas[@]} -ne 1 ]]; then
  echo "ERROR: Expected exactly 1 exported .ipa, found ${#ipas[@]}" >&2
  exit 1
fi
IPA_PATH="${ipas[0]}"
python3 ci/validate-ios-plist.py "$IPA_PATH" \
  --bundle-id "$BUNDLE_ID" --marketing-version "$SHORT_VERSION" --build-number "$BUILD_VERSION"

# 8. Upload to App Store Connect / TestFlight if enabled.
UPLOAD_STATUS="Skipped (upload_to_app_store=false)"
if [[ "$UPLOAD_TO_APP_STORE" == "true" ]]; then
  API_PRIVATE_KEYS_DIR="$SIGN_DIR" xcrun altool --upload-app \
    -f "$IPA_PATH" \
    --type ios \
    --apiKey "$APP_STORE_CONNECT_API_KEY_ID" \
    --apiIssuer "$APP_STORE_CONNECT_ISSUER_ID" 2>&1 | tee "$WORK_DIR/altool-upload.log"
  UPLOAD_STATUS="Uploaded to App Store Connect ($SHORT_VERSION build $BUILD_VERSION)"
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## KitapDuy iOS Release Summary"
    echo "| Field | Value |"
    echo "|---|---|"
    echo "| Bundle ID | \`$BUNDLE_ID\` |"
    echo "| Version | \`$SHORT_VERSION\` |"
    echo "| Build Number | \`$BUILD_VERSION\` |"
    echo "| Profile | \`$PROFILE_NAME\` (\`$PROFILE_UUID\`) |"
    echo "| IPA | \`${IPA_PATH##*/}\` |"
    echo "| App Store Connect | $UPLOAD_STATUS |"
  } >> "$GITHUB_STEP_SUMMARY"
fi
