"""Unit tests for headless iOS release validation and signing preparation."""

import base64
from datetime import datetime, timedelta, timezone
import hashlib
import os
from pathlib import Path
import plistlib
import re
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

    def test_compute_marketing_version_auto_and_override(self):
        self.assertEqual(ios_release.compute_marketing_version("1.0.2", 1), "1.0.3")
        self.assertEqual(ios_release.compute_marketing_version("1.0.2", 4), "1.0.6")
        self.assertEqual(ios_release.compute_marketing_version("1.0", 2), "1.0.2")
        self.assertEqual(
            ios_release.compute_marketing_version("1.0.2", 1, override="2.1.0"),
            "2.1.0",
        )
        for bad in ("1", "1.0-beta", "abc"):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                ios_release.compute_marketing_version("1.0.2", 1, override=bad)
        with self.assertRaises(ValueError):
            ios_release.compute_marketing_version("invalid", 1)
        with self.assertRaises(ValueError):
            ios_release.compute_marketing_version("1.0.2", 0)

    def test_update_info_plist_and_export_options(self):
        with tempfile.TemporaryDirectory() as tmp:
            auto_plist_path = Path(tmp) / "Info-auto.plist"
            auto_plist_path.write_bytes(
                (REPO_ROOT / "iosApp/iosApp/Info.plist").read_bytes()
            )
            auto_short, auto_build = ios_release.update_info_plist(
                auto_plist_path,
                run_number=3,
                override_build_number="",
                override_marketing_version="",
            )
            self.assertEqual(auto_short, "1.0.5")
            self.assertEqual(auto_build, "29")
            auto_updated = plistlib.loads(auto_plist_path.read_bytes())
            self.assertEqual(auto_updated["CFBundleVersion"], "29")
            self.assertEqual(auto_updated["CFBundleShortVersionString"], "1.0.5")

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
            self.assertEqual(updated["CFBundleLocalizations"], ["tr"])
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

    @staticmethod
    def _parse_openstep_plist(text: str) -> dict:
        tokens = []
        i = 0
        n = len(text)
        while i < n:
            if text[i].isspace():
                i += 1
            elif text[i : i + 2] == "//":
                end = text.find("\n", i)
                i = n if end == -1 else end + 1
            elif text[i : i + 2] == "/*":
                end = text.find("*/", i + 2)
                if end == -1:
                    raise ValueError("Unclosed block comment in project.pbxproj")
                i = end + 2
            elif text[i] in "{}()=;," :
                tokens.append((text[i], text[i]))
                i += 1
            elif text[i] == '"':
                j = i + 1
                out = []
                while j < n:
                    if text[j] == "\\":
                        if j + 1 >= n:
                            raise ValueError("Dangling escape in project.pbxproj")
                        out.append(text[j + 1])
                        j += 2
                    elif text[j] == '"':
                        break
                    else:
                        out.append(text[j])
                        j += 1
                else:
                    raise ValueError("Unclosed string literal in project.pbxproj")
                tokens.append(("STR", "".join(out)))
                i = j + 1
            else:
                j = i
                while j < n and not text[j].isspace() and text[j] not in '{}()=;,"':
                    if text[j : j + 2] in ("/*", "//"):
                        break
                    j += 1
                tokens.append(("ATOM", text[i:j]))
                i = j

        pos = 0

        def parse_val():
            nonlocal pos
            kind, val = tokens[pos]
            if kind == "{":
                pos += 1
                mapping = {}
                while tokens[pos][0] != "}":
                    k_kind, k_val = tokens[pos]
                    if k_kind not in ("STR", "ATOM"):
                        raise ValueError(f"Expected key, got {tokens[pos]}")
                    pos += 1
                    if tokens[pos][0] != "=":
                        raise ValueError(f"Expected '=', got {tokens[pos]}")
                    pos += 1
                    parsed = parse_val()
                    if tokens[pos][0] != ";":
                        raise ValueError(f"Expected ';', got {tokens[pos]}")
                    pos += 1
                    if k_val in mapping:
                        raise ValueError(f"Duplicate key in pbxproj dict: {k_val}")
                    mapping[k_val] = parsed
                pos += 1
                return mapping
            if kind == "(":
                pos += 1
                items = []
                while tokens[pos][0] != ")":
                    items.append(parse_val())
                    if tokens[pos][0] == ",":
                        pos += 1
                pos += 1
                return items
            if kind in ("STR", "ATOM"):
                pos += 1
                return val
            raise ValueError(f"Unexpected token {tokens[pos]}")

        root = parse_val()
        if pos != len(tokens):
            raise ValueError(f"Unexpected trailing tokens at {pos}")
        return root

    @staticmethod
    def _parse_apple_strings(text: str) -> dict[str, str]:
        cleaned = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
        entries: dict[str, str] = {}
        for line_no, raw_line in enumerate(cleaned.splitlines(), start=1):
            line = raw_line.strip()
            if not line or line.startswith("//"):
                continue
            match = re.fullmatch(
                r'"((?:\\.|[^"\\])*)"\s*=\s*"((?:\\.|[^"\\])*)";',
                line,
            )
            if not match:
                raise ValueError(f"Invalid .strings syntax on line {line_no}: {raw_line!r}")
            key = bytes(match.group(1), "utf-8").decode("unicode_escape") if "\\" in match.group(1) else match.group(1)
            val = bytes(match.group(2), "utf-8").decode("unicode_escape") if "\\" in match.group(2) else match.group(2)
            if key in entries:
                raise ValueError(f"Duplicate key in .strings file: {key!r}")
            entries[key] = val
        return entries

    def test_iosapp_turkish_localization_configuration(self):
        plist = plistlib.loads((REPO_ROOT / "iosApp/iosApp/Info.plist").read_bytes())
        self.assertEqual(plist.get("CFBundleLocalizations"), ["tr"])
        self.assertIn(plist.get("CFBundleDevelopmentRegion"), ("$(DEVELOPMENT_LANGUAGE)", "tr"))

        strings_path = REPO_ROOT / "iosApp/iosApp/tr.lproj/InfoPlist.strings"
        self.assertTrue(strings_path.is_file(), "tr.lproj/InfoPlist.strings must exist")
        raw_strings = strings_path.read_bytes()
        self.assertFalse(raw_strings.startswith(b"\xef\xbb\xbf"), "InfoPlist.strings must not contain a UTF-8 BOM")
        strings_text = raw_strings.decode("utf-8")
        self.assertNotIn("\u201c", strings_text)
        self.assertNotIn("\u201d", strings_text)
        parsed_entries = self._parse_apple_strings(strings_text)
        for key in ("NSCameraUsageDescription", "NSPhotoLibraryUsageDescription"):
            self.assertEqual(parsed_entries.get(key), plist.get(key))

        pbxproj_text = (REPO_ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
        pbxproj = self._parse_openstep_plist(pbxproj_text)
        objects = pbxproj["objects"]
        project = objects[pbxproj["rootObject"]]
        self.assertEqual(project.get("developmentRegion"), "tr")
        regions = project.get("knownRegions", [])
        self.assertIn("tr", regions)
        self.assertNotIn("en", regions)

        # Verify InfoPlist.strings variant group, file ref, build file, and Resources build phase wiring.
        iosapp_target = objects["7555FF7A242A565900829871"]
        iosapp_group = objects["7555FF7D242A565900829871"]
        variant_ids = [
            cid for cid in iosapp_group["children"]
            if objects[cid]["isa"] == "PBXVariantGroup" and objects[cid].get("name") == "InfoPlist.strings"
        ]
        self.assertEqual(len(variant_ids), 1)
        variant_id = variant_ids[0]
        variant = objects[variant_id]
        self.assertEqual(variant.get("sourceTree"), "<group>")
        self.assertEqual(len(variant["children"]), 1)
        tr_file_ref = objects[variant["children"][0]]
        self.assertEqual(tr_file_ref.get("isa"), "PBXFileReference")
        self.assertEqual(tr_file_ref.get("name"), "tr")
        self.assertEqual(tr_file_ref.get("path"), "tr.lproj/InfoPlist.strings")
        self.assertEqual(tr_file_ref.get("lastKnownFileType"), "text.plist.strings")
        self.assertTrue((REPO_ROOT / "iosApp" / iosapp_group["path"] / tr_file_ref["path"]).is_file())

        resources_phase = next(
            objects[pid] for pid in iosapp_target["buildPhases"]
            if objects[pid]["isa"] == "PBXResourcesBuildPhase"
        )
        resource_file_refs = [objects[bf_id]["fileRef"] for bf_id in resources_phase["files"]]
        self.assertIn(variant_id, resource_file_refs)

    def test_release_and_simulator_scripts_validate_turkish_localization(self):
        release_sh = (REPO_ROOT / "ci/build-ios-release.sh").read_text(encoding="utf-8")
        self.assertIn("--development-region tr", release_sh)
        self.assertIn("--localization tr", release_sh)
        self.assertIn("--exact-localizations", release_sh)

        sim_sh = (REPO_ROOT / "ci/build-ios-simulator.sh").read_text(encoding="utf-8")
        self.assertIn("--localization tr", sim_sh)
        self.assertIn("--exact-localizations", sim_sh)
        self.assertIn("KitapDuy.app", sim_sh)

        codemagic = (REPO_ROOT / "codemagic.yaml").read_text(encoding="utf-8")
        self.assertIn("--exact-localizations", codemagic)
        self.assertIn("KitapDuy.app", codemagic)

        release_yml = (REPO_ROOT / ".github/workflows/ios-release.yml").read_text(encoding="utf-8")
        self.assertIn("test_validate_ios_plist.py", release_yml)
        test_sim_sh = (REPO_ROOT / "ci/test-ios-simulator.sh").read_text(encoding="utf-8")
        self.assertIn("test_validate_ios_plist.py", test_sim_sh)

if __name__ == "__main__":
    unittest.main()
