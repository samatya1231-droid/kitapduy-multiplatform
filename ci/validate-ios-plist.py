"""Fail CI if Compose's required startup setting is missing in a plist or IPA."""
import argparse
import glob
import plistlib
import sys
import zipfile
from pathlib import Path

KEY = "CADisableMinimumFrameDurationOnPhone"


def validate(data, label, expected=None):
    plist = plistlib.loads(data)
    if not isinstance(plist, dict) or plist.get(KEY) is not True:
        raise ValueError(f"{label}: {KEY} must be a plist boolean true; Compose otherwise crashes on launch")
    for key, value in (expected or {}).items():
        if value is not None and plist.get(key) != value:
            raise ValueError(f"{label}: expected {key}={value!r}, found {plist.get(key)!r}")
    print(f"PASS: {label}: {KEY}=true")


def validate_path(path, expected=None):
    if path.suffix.lower() == ".ipa":
        with zipfile.ZipFile(path) as archive:
            entries = [name for name in archive.namelist()
                       if name.startswith("Payload/") and name.endswith(".app/Info.plist")
                       and len(name.split("/")) == 3]
            if len(entries) != 1:
                raise ValueError(f"{path}: expected exactly one main application Info.plist")
            validate(archive.read(entries[0]), f"{path}!{entries[0]}", expected)
    else:
        validate(path.read_bytes(), str(path), expected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", help="Plist/IPA paths or glob patterns")
    parser.add_argument("--bundle-id")
    parser.add_argument("--marketing-version")
    parser.add_argument("--build-number")
    args = parser.parse_args()
    expected = {
        "CFBundleIdentifier": args.bundle_id,
        "CFBundleShortVersionString": args.marketing_version,
        "CFBundleVersion": args.build_number,
    }
    try:
        for pattern in args.paths:
            matches = sorted(glob.glob(pattern))
            if not matches:
                raise ValueError(f"No files matched: {pattern}")
            for match in matches:
                validate_path(Path(match), expected)
    except (OSError, ValueError, zipfile.BadZipFile, plistlib.InvalidFileException) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
