#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

python3 -m unittest discover -b -s ci -p 'test_run_ios_smoke.py'
python3 -m unittest discover -b -s ci -p 'test_validate_ios_plist.py'
products=build/ios-simulator/Build/Products
shopt -s nullglob
test_runs=("$products"/KitapDuyUI*.xctestrun)
if [[ ${#test_runs[@]} != 1 ]]; then
  echo "Expected exactly one KitapDuyUI .xctestrun file, found ${#test_runs[@]}"
  exit 1
fi

app="$products/Release-iphonesimulator/KitapDuy.app"
# The sample backend uses loopback HTTP. Apply the exception only to this unsigned simulator copy.
python3 - "$app/Info.plist" <<'PY'
import plistlib
import sys
from pathlib import Path

info_path = Path(sys.argv[1])
info = plistlib.loads(info_path.read_bytes())
assert info["DTPlatformName"] == "iphonesimulator"
info.setdefault("NSAppTransportSecurity", {})["NSAllowsLocalNetworking"] = True
info_path.write_bytes(plistlib.dumps(info, fmt=plistlib.FMT_BINARY))
PY

python3 ci/run-ios-smoke.py \
  --app "$app" \
  --test-run "${test_runs[0]}" \
  --output build/ios-smoke
