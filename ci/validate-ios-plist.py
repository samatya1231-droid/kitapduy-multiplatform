"""Fail CI if Compose's required startup setting is missing in a plist or IPA."""
import argparse
import glob
import plistlib
import sys
import zipfile
from pathlib import Path

KEY = "CADisableMinimumFrameDurationOnPhone"


def _normalize_required_localizations(required_localizations, exact_localizations=False):
    if not required_localizations:
        if exact_localizations:
            raise ValueError("--exact-localizations requires at least one --localization")
        return []
    cleaned = []
    for item in required_localizations:
        if not isinstance(item, str) or not item.strip():
            raise ValueError("Localization codes passed to --localization must be non-empty strings")
        cleaned.append(item.strip())
    return cleaned


def _validate_lproj_entries(entry_sizes, app_prefix, required_localizations, exact_localizations, label):
    for lang in required_localizations:
        lproj_prefix = f"{lang}.lproj/"
        if not any(rel.startswith(lproj_prefix) and size > 0 for rel, size in entry_sizes.items()):
            raise ValueError(
                f"{label}: missing non-empty compiled {lang}.lproj resource directory in {app_prefix}"
            )
    if exact_localizations:
        allowed_dirs = {f"{lang}.lproj" for lang in required_localizations}
        top_level_lproj = {
            rel.split("/", 1)[0]
            for rel in entry_sizes
            if "/" in rel and rel.split("/", 1)[0].endswith(".lproj")
        }
        unexpected = sorted(top_level_lproj - allowed_dirs)
        if unexpected:
            raise ValueError(
                f"{label}: unexpected localization directory {unexpected!r} in {app_prefix}; "
                f"expected only {sorted(allowed_dirs)!r}"
            )


def validate(data, label, expected=None, required_localizations=None, exact_localizations=False):
    plist = plistlib.loads(data)
    if not isinstance(plist, dict) or plist.get(KEY) is not True:
        raise ValueError(f"{label}: {KEY} must be a plist boolean true; Compose otherwise crashes on launch")
    for key, value in (expected or {}).items():
        if value is not None and plist.get(key) != value:
            raise ValueError(f"{label}: expected {key}={value!r}, found {plist.get(key)!r}")
    cleaned = _normalize_required_localizations(
        required_localizations, exact_localizations=exact_localizations
    )
    if cleaned:
        localizations = plist.get("CFBundleLocalizations")
        if (
            not isinstance(localizations, list)
            or not all(isinstance(item, str) and item.strip() for item in localizations)
        ):
            raise ValueError(
                f"{label}: CFBundleLocalizations must be an array of non-empty strings, found {localizations!r}"
            )
        if len(set(localizations)) != len(localizations):
            raise ValueError(
                f"{label}: CFBundleLocalizations contains duplicate entries: {localizations!r}"
            )
        missing = [lang for lang in cleaned if lang not in localizations]
        if missing:
            raise ValueError(
                f"{label}: CFBundleLocalizations missing {missing!r}; found {localizations!r}"
            )
        if exact_localizations:
            extra = [lang for lang in localizations if lang not in cleaned]
            if extra:
                raise ValueError(
                    f"{label}: CFBundleLocalizations contains unexpected entries {extra!r}; "
                    f"expected only {cleaned!r}"
                )
    print(f"PASS: {label}: {KEY}=true")


def validate_path(path, expected=None, required_localizations=None, exact_localizations=False):
    cleaned_localizations = _normalize_required_localizations(
        required_localizations, exact_localizations=exact_localizations
    )

    if path.suffix.lower() == ".ipa":
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            entries = [name for name in names
                       if name.startswith("Payload/") and name.endswith(".app/Info.plist")
                       and len(name.split("/")) == 3]
            if len(entries) != 1:
                raise ValueError(f"{path}: expected exactly one main application Info.plist")
            plist_entry = entries[0]
            validate(
                archive.read(plist_entry),
                f"{path}!{plist_entry}",
                expected,
                required_localizations=cleaned_localizations,
                exact_localizations=exact_localizations,
            )
            if cleaned_localizations:
                app_prefix = plist_entry[:-len("Info.plist")]
                entry_sizes = {
                    info.filename[len(app_prefix):]: info.file_size
                    for info in archive.infolist()
                    if info.filename.startswith(app_prefix)
                    and not info.is_dir()
                    and not info.filename.endswith("/")
                }
                _validate_lproj_entries(
                    entry_sizes,
                    app_prefix,
                    cleaned_localizations,
                    exact_localizations,
                    str(path),
                )
    elif path.is_dir() and path.suffix.lower() == ".app":
        plist_path = path / "Info.plist"
        if not plist_path.is_file():
            raise ValueError(f"{path}: missing Info.plist in .app bundle")
        validate(
            plist_path.read_bytes(),
            str(plist_path),
            expected,
            required_localizations=cleaned_localizations,
            exact_localizations=exact_localizations,
        )
        if cleaned_localizations:
            entry_sizes = {
                item.relative_to(path).as_posix(): item.stat().st_size
                for item in path.rglob("*")
                if item.is_file()
            }
            _validate_lproj_entries(
                entry_sizes,
                f"{path.name}/",
                cleaned_localizations,
                exact_localizations,
                str(path),
            )
    else:
        validate(
            path.read_bytes(),
            str(path),
            expected,
            required_localizations=cleaned_localizations,
            exact_localizations=exact_localizations,
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", help="Plist, .app bundle, or .ipa paths/glob patterns")
    parser.add_argument("--bundle-id")
    parser.add_argument("--marketing-version")
    parser.add_argument("--build-number")
    parser.add_argument("--development-region")
    parser.add_argument("--localization", action="append", default=[])
    parser.add_argument(
        "--exact-localizations",
        action="store_true",
        help="Reject extra CFBundleLocalizations entries or top-level .lproj directories",
    )
    args = parser.parse_args()
    expected = {
        "CFBundleIdentifier": args.bundle_id,
        "CFBundleShortVersionString": args.marketing_version,
        "CFBundleVersion": args.build_number,
        "CFBundleDevelopmentRegion": args.development_region,
    }
    try:
        for pattern in args.paths:
            matches = sorted(glob.glob(pattern))
            if not matches:
                raise ValueError(f"No files matched: {pattern}")
            for match in matches:
                validate_path(
                    Path(match),
                    expected,
                    required_localizations=args.localization,
                    exact_localizations=args.exact_localizations,
                )
    except (OSError, ValueError, zipfile.BadZipFile, plistlib.InvalidFileException) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
