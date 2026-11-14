"""Unit tests for headless iOS release validation and signing preparation."""

import base64
from datetime import datetime, timedelta, timezone
import hashlib
import os
from pathlib import Path
import plistlib
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
import ios_release  # noqa: E402


REPO_ROOT = Path(__file__).resolve().parents[1]


def make_profile_plist(
    *,
    team_id: str = "ABC123DEF4",
    bundle_id: str = "com.semtinazizi.kitapduy",
    name: str = "Kitapduy Yayinlama",
    uuid: str = "11111111-2222-3333-4444-555555555555",
    expired: bool = False,
    dev_devices: bool = False,
    get_task_allow: bool = False,
    aps_env: str | None = "production",
    apple_signin: bool = True,
    cert_bytes: bytes = b"dummy-der-cert",
) -> bytes:
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    exp = now - timedelta(days=1) if expired else now + timedelta(days=90)
    entitlements = {
        "application-identifier": f"{team_id}.{bundle_id}",
        "com.apple.developer.team-identifier": team_id,
    }
    if get_task_allow:
        entitlements["get-task-allow"] = True
    if aps_env is not None:
        entitlements["aps-environment"] = aps_env
    if apple_signin:
        entitlements["com.apple.developer.applesignin"] = ["Default"]
    payload = {
        "UUID": uuid,
        "Name": name,
        "TeamIdentifier": [team_id],
        "ExpirationDate": exp,
        "Entitlements": entitlements,
        "DeveloperCertificates": [cert_bytes] if cert_bytes else [],
    }
    if dev_devices:
        payload["ProvisionedDevices"] = ["00008110-000000000000001E"]
    return plistlib.dumps(payload, fmt=plistlib.FMT_XML)


class SecretDecodingTests(unittest.TestCase):
    def test_decode_base64_secret_accepts_wrapped_whitespace(self):
        raw_bytes = b"\x30\x82\x01\x02p12-data"
        encoded = base64.b64encode(raw_bytes).decode()
        wrapped = f"  {encoded[:6]}\n{encoded[6:]}  \n"
        self.assertEqual(ios_release.decode_base64_secret(wrapped, "TEST_SECRET"), raw_bytes)

    def test_decode_base64_secret_rejects_empty_and_invalid(self):
        for bad in ("", "   ", "not-valid-base64!@#"):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                ios_release.decode_base64_secret(bad, "TEST_SECRET")

    def test_decode_p8_secret_accepts_both_raw_pem_and_base64(self):
        pem = "-----BEGIN PRIVATE KEY-----\nABC123\n-----END PRIVATE KEY-----\n"
        self.assertEqual(ios_release.decode_p8_secret(pem).decode(), pem)
        b64 = base64.b64encode(pem.encode()).decode()
        self.assertEqual(ios_release.decode_p8_secret(b64).decode(), pem)
        bom_pem = "\ufeff" + pem
        self.assertEqual(ios_release.decode_p8_secret(bom_pem).decode(), pem)
        bom_b64 = base64.b64encode(bom_pem.encode("utf-8")).decode()
        self.assertEqual(ios_release.decode_p8_secret(bom_b64).decode(), pem)

    def test_decode_p8_secret_rejects_non_p8_payload(self):
        bad_b64 = base64.b64encode(b"hello world").decode()
        with self.assertRaises(ValueError):
            ios_release.decode_p8_secret(bad_b64)

    def test_decode_secrets_cli_writes_expected_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            pem = "-----BEGIN PRIVATE KEY-----\nXYZ\n-----END PRIVATE KEY-----\n"
            env = {
                "IOS_BUILD_CERTIFICATE_BASE64": base64.b64encode(b"p12-bytes").decode(),
                "IOS_P12_PASSWORD": "secret-pass",
                "IOS_PROVISION_PROFILE_BASE64": base64.b64encode(b"profile-bytes").decode(),
                "APP_STORE_CONNECT_API_KEY_ID": "ABC123DEF4",
                "APP_STORE_CONNECT_ISSUER_ID": "11111111-2222-3333-4444-555555555555",
                "APP_STORE_CONNECT_API_KEY_BASE64": base64.b64encode(pem.encode()).decode(),
                "UPLOAD_TO_APP_STORE": "true",
            }
            with patch.dict(os.environ, env, clear=False):
                rc = ios_release.main(["decode-secrets", "--out-dir", tmp])
            self.assertEqual(rc, 0)
            self.assertEqual((Path(tmp) / "distribution.p12").read_bytes(), b"p12-bytes")
            self.assertEqual((Path(tmp) / "profile.mobileprovision").read_bytes(), b"profile-bytes")
            self.assertEqual((Path(tmp) / "AuthKey_ABC123DEF4.p8").read_text(), pem)

    def test_decode_secrets_skips_asc_key_when_upload_disabled(self):
        with tempfile.TemporaryDirectory() as tmp:
            env = {
                "IOS_BUILD_CERTIFICATE_BASE64": base64.b64encode(b"p12-bytes").decode(),
                "IOS_P12_PASSWORD": "secret-pass",
                "IOS_PROVISION_PROFILE_BASE64": base64.b64encode(b"profile-bytes").decode(),
                "APP_STORE_CONNECT_API_KEY_ID": "",
                "APP_STORE_CONNECT_ISSUER_ID": "",
                "APP_STORE_CONNECT_API_KEY_BASE64": "",
                "UPLOAD_TO_APP_STORE": "false",
            }
            with patch.dict(os.environ, env, clear=False):
                rc = ios_release.main(["decode-secrets", "--out-dir", tmp])
            self.assertEqual(rc, 0)
            self.assertTrue((Path(tmp) / "distribution.p12").is_file())


class ProfileValidationTests(unittest.TestCase):
    def test_valid_app_store_profile_and_cert_match(self):
        cert = b"apple-dist-cert-der"
        info = ios_release.parse_mobileprovision_plist(make_profile_plist(cert_bytes=cert))
        self.assertEqual(info["team_id"], "ABC123DEF4")
        self.assertEqual(info["name"], "Kitapduy Yayinlama")

        sha256_hex = hashlib.sha256(cert).hexdigest()
        expected_sha1 = hashlib.sha1(cert).hexdigest().upper()
        self.assertEqual(ios_release.match_profile_certificate([sha256_hex], info), expected_sha1)

    def test_rejects_expired_dev_wrong_bundle_and_missing_entitlements(self):
        cases = [
            {"expired": True},
            {"dev_devices": True},
            {"get_task_allow": True},
            {"bundle_id": "com.other.app"},
            {"aps_env": "development"},
            {"aps_env": None},
            {"apple_signin": False},
            {"cert_bytes": b""},
        ]
        for kwargs in cases:
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                ios_release.parse_mobileprovision_plist(make_profile_plist(**kwargs))

        empty_signin = plistlib.loads(make_profile_plist())
        empty_signin["Entitlements"]["com.apple.developer.applesignin"] = []
        with self.assertRaises(ValueError):
            ios_release.parse_mobileprovision_plist(plistlib.dumps(empty_signin))

    def test_mismatched_certificate_fails_with_clear_message(self):
        info = ios_release.parse_mobileprovision_plist(make_profile_plist(cert_bytes=b"cert-a"))
        other_sha256 = hashlib.sha256(b"cert-b").hexdigest().upper()
        with self.assertRaisesRegex(ValueError, "does not match"):
            ios_release.match_profile_certificate([other_sha256], info)

    def test_match_profile_certificate_prefers_valid_identity_over_keyless_cert(self):
        cert_a = b"cert-keyless"
        cert_b = b"cert-with-private-key"
        profile_dict = plistlib.loads(make_profile_plist(cert_bytes=cert_a))
        profile_dict["DeveloperCertificates"] = [cert_a, cert_b]
        info = ios_release.parse_mobileprovision_plist(plistlib.dumps(profile_dict))

        installed_sha256s = [
            hashlib.sha256(cert_a).hexdigest().upper(),
            hashlib.sha256(cert_b).hexdigest().upper(),
        ]
        identity_sha1_b = hashlib.sha1(cert_b).hexdigest().upper()
        self.assertEqual(
            ios_release.match_profile_certificate(
                installed_sha256s, info, identity_sha1s=[identity_sha1_b]
            ),
            identity_sha1_b,
        )


class VersionAndExportTests(unittest.TestCase):
    def test_compute_build_version_auto_and_override(self):
        self.assertEqual(ios_release.compute_build_version("26", 1), "27")
        self.assertEqual(ios_release.compute_build_version("26", 5), "31")
        self.assertEqual(ios_release.compute_build_version("26", 1, override="100"), "100")
        for bad in ("26-beta", "0", "0.0"):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                ios_release.compute_build_version("26", 1, override=bad)

    def test_update_info_plist_and_export_options(self):
        with tempfile.TemporaryDirectory() as tmp:
            plist_path = Path(tmp) / "Info.plist"
            plist_path.write_bytes(
                (REPO_ROOT / "iosApp/iosApp/Info.plist").read_bytes()
            )
            short_ver, build_ver = ios_release.update_info_plist(
                plist_path,
                run_number=3,
                override_build_number="",
                override_marketing_version="1.0.3",
            )
            self.assertEqual(short_ver, "1.0.3")
            self.assertEqual(build_ver, "29")
            updated = plistlib.loads(plist_path.read_bytes())
            self.assertEqual(updated["CFBundleVersion"], "29")
            self.assertEqual(updated["CFBundleShortVersionString"], "1.0.3")
            self.assertIs(updated["CADisableMinimumFrameDurationOnPhone"], True)

            export_bytes = ios_release.build_export_options_plist(
                team_id="ABC123DEF4",
                bundle_id="com.semtinazizi.kitapduy",
                profile_name="Kitapduy Yayinlama",
                signing_certificate="DEADBEEF" * 5,
            )
            export_plist = plistlib.loads(export_bytes)
            self.assertEqual(export_plist["method"], "app-store-connect")
            self.assertEqual(export_plist["destination"], "export")
            self.assertEqual(export_plist["signingStyle"], "manual")
            self.assertEqual(
                export_plist["provisioningProfiles"]["com.semtinazizi.kitapduy"],
                "Kitapduy Yayinlama",
            )
            self.assertIs(export_plist["manageAppVersionAndBuildNumber"], False)


class XcodeProjectSanityTests(unittest.TestCase):
    def test_iosapp_scheme_exists_and_archives_release(self):
        scheme_path = REPO_ROOT / "iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme"
        self.assertTrue(scheme_path.is_file(), "iosApp.xcscheme is required for xcodebuild archive")
        root = ET.parse(scheme_path).getroot()
        entry = root.find("./BuildAction/BuildActionEntries/BuildActionEntry")
        self.assertIsNotNone(entry)
        self.assertEqual(entry.attrib.get("buildForArchiving"), "YES")
        ref = entry.find("BuildableReference")
        self.assertEqual(ref.attrib.get("BlueprintName"), "iosApp")
        self.assertEqual(ref.attrib.get("BlueprintIdentifier"), "7555FF7A242A565900829871")
        archive = root.find("./ArchiveAction")
        self.assertEqual(archive.attrib.get("buildConfiguration"), "Release")

    def test_pbxproj_does_not_append_team_id_to_bundle_id(self):
        pbxproj = (REPO_ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
        self.assertNotIn("${BUNDLE_ID}${TEAM_ID}", pbxproj)
        self.assertIn('PRODUCT_BUNDLE_IDENTIFIER = "${BUNDLE_ID}";', pbxproj)
        self.assertIn('PROVISIONING_PROFILE_SPECIFIER = "${APP_PROVISIONING_PROFILE_SPECIFIER}";', pbxproj)

    def test_workflows_and_gitignore_match_release_contract(self):
        release_yml = (REPO_ROOT / ".github/workflows/ios-release.yml").read_text(encoding="utf-8")
        qa_yml = (REPO_ROOT / ".github/workflows/ios-qa.yml").read_text(encoding="utf-8")
        for yml in (release_yml, qa_yml):
            self.assertIn("runs-on: macos-26", yml)
            self.assertIn("~/.konan", yml)
            self.assertIn(".spm-cache", yml)
            self.assertIn("cache: gradle", yml)
        for secret in (
            "IOS_BUILD_CERTIFICATE_BASE64",
            "IOS_P12_PASSWORD",
            "IOS_PROVISION_PROFILE_BASE64",
            "APP_STORE_CONNECT_API_KEY_ID",
            "APP_STORE_CONNECT_ISSUER_ID",
            "APP_STORE_CONNECT_API_KEY_BASE64",
        ):
            self.assertIn(f"secrets.{secret}", release_yml)

        gitignore = (REPO_ROOT / ".gitignore").read_text(encoding="utf-8")
        for pattern in ("*.p12", "*.p8", "*.mobileprovision", ".spm-cache/"):
            self.assertIn(pattern, gitignore)

if __name__ == "__main__":
    unittest.main()
