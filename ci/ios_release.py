"""Helpers and validation for headless GitHub Actions iOS App Store releases."""

import argparse
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import shlex
import sys
from uuid import UUID

DEFAULT_BUNDLE_ID = "com.semtinazizi.kitapduy"


def decode_base64_secret(raw: str, secret_name: str) -> bytes:
    """Decode a Base64 GitHub secret while rejecting empty or malformed input."""
    compact = "".join((raw or "").split())
    if not compact:
        raise ValueError(f"Missing required secret: {secret_name}")
    try:
        decoded = base64.b64decode(compact, validate=True)
    except Exception as exc:
        raise ValueError(
            f"Secret {secret_name} is not valid Base64. Re-encode the original file without extra text."
        ) from exc
    if not decoded:
        raise ValueError(f"Secret {secret_name} decoded to 0 bytes.")
    return decoded


def decode_p8_secret(raw: str, secret_name: str = "APP_STORE_CONNECT_API_KEY_BASE64") -> bytes:
    """Accept either raw PEM text or Base64-encoded contents of an AuthKey_*.p8 file."""
    stripped = (raw or "").lstrip("\ufeff").strip()
    if not stripped:
        raise ValueError(f"Missing required secret: {secret_name}")
    if "-----BEGIN PRIVATE KEY-----" in stripped:
        data = (stripped.replace("\r\n", "\n") + "\n").encode("utf-8")
    else:
        data = decode_base64_secret(stripped, secret_name)
    text = data.decode("utf-8", errors="replace").lstrip("\ufeff").replace("\r\n", "\n")
    if "-----BEGIN PRIVATE KEY-----" not in text or "-----END PRIVATE KEY-----" not in text:
        raise ValueError(
            f"Secret {secret_name} does not look like an App Store Connect .p8 private key."
        )
    return text.rstrip().encode("utf-8") + b"\n"


def _utc_naive(dt: datetime) -> datetime:
    if dt.tzinfo is not None:
        return dt.astimezone(timezone.utc).replace(tzinfo=None)
    return dt


def parse_mobileprovision_plist(
    plist_bytes: bytes,
    expected_bundle_id: str = DEFAULT_BUNDLE_ID,
    now: datetime | None = None,
) -> dict:
    """Validate an App Store distribution provisioning profile plist and extract metadata."""
    try:
        plist = plistlib.loads(plist_bytes)
    except Exception as exc:
        raise ValueError("Failed to parse decoded provisioning profile plist.") from exc
    if not isinstance(plist, dict):
        raise ValueError("Provisioning profile plist must be a dictionary.")

    uuid = str(plist.get("UUID") or "").strip()
    name = str(plist.get("Name") or "").strip()
    if not uuid or not name:
        raise ValueError("Provisioning profile is missing UUID or Name.")
    if str(UUID(uuid)).lower() != uuid.lower():
        raise ValueError("Provisioning profile UUID must use the standard hyphenated format.")

    exp = plist.get("ExpirationDate")
    if not isinstance(exp, datetime):
        raise ValueError(f"Provisioning profile '{name}' is missing ExpirationDate.")
    current_time = _utc_naive(now or datetime.now(timezone.utc))
    if _utc_naive(exp) <= current_time:
        raise ValueError(f"Provisioning profile '{name}' expired on {exp.isoformat()}.")

    if plist.get("ProvisionedDevices") or plist.get("ProvisionsAllDevices"):
        raise ValueError(
            f"Provisioning profile '{name}' is not an App Store distribution profile "
            "(found ProvisionedDevices / ProvisionsAllDevices)."
        )

    entitlements = plist.get("Entitlements")
    if not isinstance(entitlements, dict):
        raise ValueError(f"Provisioning profile '{name}' is missing Entitlements.")
    if entitlements.get("get-task-allow") is True:
        raise ValueError(
            f"Provisioning profile '{name}' has get-task-allow=true (Development profile, not App Store)."
        )

    team_ids = plist.get("TeamIdentifier") or []
    if not isinstance(team_ids, list) or not team_ids or not str(team_ids[0]).strip():
        raise ValueError(f"Provisioning profile '{name}' is missing TeamIdentifier.")
    team_id = str(team_ids[0]).strip()

    app_id = str(
        entitlements.get("application-identifier")
        or entitlements.get("com.apple.application-identifier")
        or ""
    ).strip()
    expected_app_id = f"{team_id}.{expected_bundle_id}"
    if app_id != expected_app_id:
        raise ValueError(
            f"Provisioning profile '{name}' application-identifier '{app_id}' "
            f"does not match '{expected_app_id}'."
        )

    if entitlements.get("aps-environment") != "production":
        raise ValueError(
            f"Provisioning profile '{name}' must have aps-environment='production' "
            "(enable Push Notifications on the App ID and regenerate the App Store profile)."
        )

    if not entitlements.get("com.apple.developer.applesignin"):
        raise ValueError(
            f"Provisioning profile '{name}' is missing Sign in with Apple entitlement "
            "(com.apple.developer.applesignin)."
        )

    certs = plist.get("DeveloperCertificates") or []
    if not isinstance(certs, list) or not certs:
        raise ValueError(f"Provisioning profile '{name}' contains no DeveloperCertificates.")

    sha256_to_sha1 = {}
    for cert_bytes in certs:
        if not isinstance(cert_bytes, (bytes, bytearray)):
            continue
        sha256_hex = hashlib.sha256(cert_bytes).hexdigest().upper()
        sha1_hex = hashlib.sha1(cert_bytes).hexdigest().upper()
        sha256_to_sha1[sha256_hex] = sha1_hex

    if not sha256_to_sha1:
        raise ValueError(f"Provisioning profile '{name}' had no readable certificate entries.")

    return {
        "uuid": uuid,
        "name": name,
        "team_id": team_id,
        "bundle_id": expected_bundle_id,
        "expiration_date": _utc_naive(exp).isoformat() + "Z",
        "sha256_to_sha1": sha256_to_sha1,
    }


def normalize_hex(value: str) -> str:
    return re.sub(r"[^0-9A-Fa-f]", "", value or "").upper()


def match_profile_certificate(
    installed_sha256s: list[str],
    profile_info: dict,
    identity_sha1s: list[str] | None = None,
) -> str:
    """Return the SHA-1 of the installed certificate that matches the provisioning profile."""
    mapping = profile_info.get("sha256_to_sha1") or {}
    profile_sha1s = {normalize_hex(sha1) for sha1 in mapping.values() if normalize_hex(sha1)}
    if identity_sha1s:
        for raw_sha1 in identity_sha1s:
            norm_sha1 = normalize_hex(raw_sha1)
            if norm_sha1 in profile_sha1s:
                return norm_sha1
    for raw_digest in installed_sha256s:
        norm = normalize_hex(raw_digest)
        if norm in mapping:
            return mapping[norm]
    raise ValueError(
        f"The imported .p12 certificate does not match any certificate in provisioning profile "
        f"'{profile_info.get('name')}'. Edit/regenerate the App Store profile in Apple Developer "
        "Portal to select the certificate from your .p12 and re-download the .mobileprovision."
    )


def compute_build_version(current_version: str, run_number: int, override: str = "") -> str:
    """Compute a strictly valid, monotonic CFBundleVersion."""
    explicit = (override or "").strip()
    if explicit:
        if not re.fullmatch(r"\d+(?:\.\d+){0,2}", explicit) or not any(
            int(part) > 0 for part in explicit.split(".")
        ):
            raise ValueError(
                f"Invalid build_number '{explicit}'. Use positive integers like '27' or '1.0.27'."
            )
        return explicit

    cur = (current_version or "").strip()
    if not re.fullmatch(r"\d+", cur):
        raise ValueError(
            f"Current CFBundleVersion '{cur}' is not an integer; specify build_number input explicitly."
        )
    if run_number < 1:
        raise ValueError(f"run_number must be >= 1, got {run_number}")
    return str(int(cur) + run_number)


def update_info_plist(
    plist_path: Path,
    run_number: int,
    override_build_number: str = "",
    override_marketing_version: str = "",
) -> tuple[str, str]:
    """Update CFBundleVersion (and optionally CFBundleShortVersionString) in Info.plist."""
    plist = plistlib.loads(plist_path.read_bytes())
    if not isinstance(plist, dict):
        raise ValueError(f"{plist_path}: Info.plist must be a dictionary")
    if plist.get("CADisableMinimumFrameDurationOnPhone") is not True:
        raise ValueError(
            f"{plist_path}: CADisableMinimumFrameDurationOnPhone must remain boolean true"
        )

    new_build = compute_build_version(
        str(plist.get("CFBundleVersion") or ""),
        run_number=run_number,
        override=override_build_number,
    )
    plist["CFBundleVersion"] = new_build

    explicit_short = (override_marketing_version or "").strip()
    if explicit_short:
        if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", explicit_short):
            raise ValueError(
                f"Invalid marketing_version '{explicit_short}'. Expected format like '1.0.3'."
            )
        plist["CFBundleShortVersionString"] = explicit_short

    short_version = str(plist.get("CFBundleShortVersionString") or "").strip()
    if not short_version:
        raise ValueError(f"{plist_path}: CFBundleShortVersionString is empty")

    plist_path.write_bytes(plistlib.dumps(plist, fmt=plistlib.FMT_XML))
    return short_version, new_build


def build_export_options_plist(
    team_id: str,
    bundle_id: str,
    profile_name: str,
    signing_certificate: str = "Apple Distribution",
) -> bytes:
    """Create an App Store Connect manual signing ExportOptions.plist."""
    payload = {
        "method": "app-store-connect",
        "destination": "export",
        "signingStyle": "manual",
        "teamID": team_id,
        "signingCertificate": signing_certificate,
        "provisioningProfiles": {bundle_id: profile_name},
        "uploadSymbols": True,
        "manageAppVersionAndBuildNumber": False,
    }
    return plistlib.dumps(payload, fmt=plistlib.FMT_XML)


def _write_secure(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    try:
        os.chmod(path, 0o600)
    except OSError:
        pass


def cmd_decode_secrets(args: argparse.Namespace) -> int:
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    key_id = (os.environ.get("APP_STORE_CONNECT_API_KEY_ID") or "").strip()
    issuer_id = (os.environ.get("APP_STORE_CONNECT_ISSUER_ID") or "").strip()
    p12_pass = os.environ.get("IOS_P12_PASSWORD") or ""
    if not p12_pass:
        raise ValueError("Missing required secret: IOS_P12_PASSWORD")

    p12_bytes = decode_base64_secret(
        os.environ.get("IOS_BUILD_CERTIFICATE_BASE64", ""),
        "IOS_BUILD_CERTIFICATE_BASE64",
    )
    profile_bytes = decode_base64_secret(
        os.environ.get("IOS_PROVISION_PROFILE_BASE64", ""),
        "IOS_PROVISION_PROFILE_BASE64",
    )

    _write_secure(out_dir / "distribution.p12", p12_bytes)
    _write_secure(out_dir / "profile.mobileprovision", profile_bytes)

    upload = os.environ.get("UPLOAD_TO_APP_STORE", "false") == "true"
    if upload:
        if not re.fullmatch(r"[A-Za-z0-9]{8,20}", key_id):
            raise ValueError(
                "Missing or invalid secret: APP_STORE_CONNECT_API_KEY_ID (expected e.g. 10-character Key ID)."
            )
        if not re.fullmatch(
            r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            issuer_id,
        ):
            raise ValueError(
                "Missing or invalid secret: APP_STORE_CONNECT_ISSUER_ID (expected UUID)."
            )
        p8_bytes = decode_p8_secret(os.environ.get("APP_STORE_CONNECT_API_KEY_BASE64", ""))
        _write_secure(out_dir / f"AuthKey_{key_id}.p8", p8_bytes)

    return 0


def cmd_validate_profile(args: argparse.Namespace) -> int:
    info = parse_mobileprovision_plist(
        Path(args.profile_plist).read_bytes(),
        expected_bundle_id=args.bundle_id,
    )
    Path(args.out_json).write_text(json.dumps(info, indent=2), encoding="utf-8")
    return 0


def cmd_match_cert(args: argparse.Namespace) -> int:
    if not args.cert_sha256 and not args.identity_sha1:
        raise ValueError("Provide at least one --cert-sha256 or --identity-sha1 digest.")
    info = json.loads(Path(args.profile_json).read_text(encoding="utf-8"))
    sha1 = match_profile_certificate(
        args.cert_sha256,
        info,
        identity_sha1s=args.identity_sha1,
    )
    print(sha1)
    return 0


def cmd_prepare_build(args: argparse.Namespace) -> int:
    info = json.loads(Path(args.profile_json).read_text(encoding="utf-8"))
    short_ver, build_ver = update_info_plist(
        Path(args.info_plist),
        run_number=args.run_number,
        override_build_number=args.build_number,
        override_marketing_version=args.marketing_version,
    )
    export_bytes = build_export_options_plist(
        team_id=info["team_id"],
        bundle_id=info["bundle_id"],
        profile_name=info["name"],
        signing_certificate=args.signing_cert,
    )
    export_path = Path(args.export_options)
    export_path.parent.mkdir(parents=True, exist_ok=True)
    export_path.write_bytes(export_bytes)
    print(f"SHORT_VERSION={shlex.quote(short_ver)}")
    print(f"BUILD_VERSION={shlex.quote(build_ver)}")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    p_dec = sub.add_parser("decode-secrets")
    p_dec.add_argument("--out-dir", required=True)
    p_dec.set_defaults(func=cmd_decode_secrets)

    p_val = sub.add_parser("validate-profile")
    p_val.add_argument("--profile-plist", required=True)
    p_val.add_argument("--bundle-id", default=DEFAULT_BUNDLE_ID)
    p_val.add_argument("--out-json", required=True)
    p_val.set_defaults(func=cmd_validate_profile)

    p_match = sub.add_parser("match-cert")
    p_match.add_argument("--profile-json", required=True)
    p_match.add_argument("--cert-sha256", nargs="*", default=[])
    p_match.add_argument("--identity-sha1", nargs="*", default=[])
    p_match.set_defaults(func=cmd_match_cert)

    p_prep = sub.add_parser("prepare-build")
    p_prep.add_argument("--info-plist", required=True)
    p_prep.add_argument("--run-number", type=int, required=True)
    p_prep.add_argument("--build-number", default="")
    p_prep.add_argument("--marketing-version", default="")
    p_prep.add_argument("--export-options", required=True)
    p_prep.add_argument("--profile-json", required=True)
    p_prep.add_argument("--signing-cert", default="Apple Distribution")
    p_prep.set_defaults(func=cmd_prepare_build)

    args = parser.parse_args(argv)
    try:
        return args.func(args)
    except (OSError, ValueError, KeyError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
