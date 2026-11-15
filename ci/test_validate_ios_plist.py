import importlib.util
import plistlib
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).with_name("validate-ios-plist.py")
spec = importlib.util.spec_from_file_location("validator", SCRIPT)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class PlistValidationTests(unittest.TestCase):
    def test_xml_and_binary_true(self):
        for fmt in (plistlib.FMT_XML, plistlib.FMT_BINARY):
            validator.validate(plistlib.dumps({validator.KEY: True}, fmt=fmt), "fixture")

    def test_missing_false_and_wrong_types(self):
        for value in (None, False, "true", "YES", 1):
            data = {} if value is None else {validator.KEY: value}
            with self.subTest(value=value), self.assertRaises(ValueError):
                validator.validate(plistlib.dumps(data), "fixture")

    def test_ipa_reads_main_binary_plist(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "KitapDuy.ipa"
            for valid in (True, False):
                with zipfile.ZipFile(path, "w") as archive:
                    archive.writestr("Payload/KitapDuy.app/Info.plist", plistlib.dumps(
                        {validator.KEY: valid}, fmt=plistlib.FMT_BINARY))
                    archive.writestr("Payload/KitapDuy.app/Frameworks/Other.app/Info.plist", b"ignored")
                result = subprocess.run([sys.executable, str(SCRIPT), str(path)], capture_output=True)
                self.assertEqual(result.returncode, 0 if valid else 1, result.stderr)

    def test_ipa_rejects_wrong_build_number_before_upload(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "KitapDuy.ipa"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("Payload/KitapDuy.app/Info.plist", plistlib.dumps({
                    validator.KEY: True,
                    "CFBundleIdentifier": "com.semtinazizi.kitapduy",
                    "CFBundleShortVersionString": "1.0.2",
                    "CFBundleVersion": "27",
                }))
            result = subprocess.run([
                sys.executable, str(SCRIPT), str(path), "--build-number", "28",
            ], capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertIn("CFBundleVersion", result.stderr)

    def test_missing_artifact_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([sys.executable, str(SCRIPT), str(Path(directory) / "*.ipa")], capture_output=True)
            self.assertEqual(result.returncode, 1)

    def test_ipa_without_main_plist_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "empty.ipa"
            with zipfile.ZipFile(path, "w"):
                pass
            with self.assertRaises(ValueError):
                validator.validate_path(path)

    def test_development_region_and_localizations_on_plist(self):
        valid_plist = plistlib.dumps({
            validator.KEY: True,
            "CFBundleDevelopmentRegion": "tr",
            "CFBundleLocalizations": ["tr"],
        })
        validator.validate(
            valid_plist,
            "fixture",
            expected={"CFBundleDevelopmentRegion": "tr"},
            required_localizations=["tr"],
            exact_localizations=True,
        )

        for bad_region in ("en", "$(DEVELOPMENT_LANGUAGE)", None):
            payload = {validator.KEY: True, "CFBundleLocalizations": ["tr"]}
            if bad_region is not None:
                payload["CFBundleDevelopmentRegion"] = bad_region
            with self.subTest(bad_region=bad_region), self.assertRaises(ValueError):
                validator.validate(
                    plistlib.dumps(payload),
                    "fixture",
                    expected={"CFBundleDevelopmentRegion": "tr"},
                    required_localizations=["tr"],
                )

        for bad_locs in (None, [], ["en"], ["tr", ""], ["tr", "tr"], "tr", [123]):
            payload = {validator.KEY: True, "CFBundleDevelopmentRegion": "tr"}
            if bad_locs is not None:
                payload["CFBundleLocalizations"] = bad_locs
            with self.subTest(bad_locs=bad_locs), self.assertRaises(ValueError):
                validator.validate(
                    plistlib.dumps(payload),
                    "fixture",
                    expected={"CFBundleDevelopmentRegion": "tr"},
                    required_localizations=["tr"],
                )

        extra_en_plist = plistlib.dumps({
            validator.KEY: True,
            "CFBundleDevelopmentRegion": "tr",
            "CFBundleLocalizations": ["en", "tr"],
        })
        with self.assertRaises(ValueError):
            validator.validate(
                extra_en_plist,
                "fixture",
                expected={"CFBundleDevelopmentRegion": "tr"},
                required_localizations=["tr"],
                exact_localizations=True,
            )

        for bad_req in ([""], [123]):
            with self.subTest(bad_req=bad_req), self.assertRaises(ValueError):
                validator.validate(valid_plist, "fixture", required_localizations=bad_req)

        with self.assertRaises(ValueError):
            validator.validate(valid_plist, "fixture", exact_localizations=True)

    def test_ipa_requires_compiled_lproj_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            plist_bytes = plistlib.dumps({
                validator.KEY: True,
                "CFBundleIdentifier": "com.semtinazizi.kitapduy",
                "CFBundleShortVersionString": "1.0.2",
                "CFBundleVersion": "27",
                "CFBundleDevelopmentRegion": "tr",
                "CFBundleLocalizations": ["tr"],
            }, fmt=plistlib.FMT_BINARY)

            valid_ipa = Path(directory) / "valid.ipa"
            with zipfile.ZipFile(valid_ipa, "w") as archive:
                archive.writestr("Payload/KitapDuy.app/Info.plist", plist_bytes)
                archive.writestr("Payload/KitapDuy.app/tr.lproj/InfoPlist.strings", b"bplist00")
                archive.writestr("Payload/KitapDuy.app/GoogleUserMessagingPlatform.bundle/en.lproj/Localizable.strings", b"bplist00")
                archive.writestr("Payload/KitapDuy.app/Frameworks/Other.framework/en.lproj/Localizable.strings", b"bplist00")
            result = subprocess.run([
                sys.executable, str(SCRIPT), str(valid_ipa),
                "--bundle-id", "com.semtinazizi.kitapduy",
                "--marketing-version", "1.0.2",
                "--build-number", "27",
                "--development-region", "tr",
                "--localization", "tr",
                "--exact-localizations",
            ], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)

            missing_lproj_ipa = Path(directory) / "missing-lproj.ipa"
            with zipfile.ZipFile(missing_lproj_ipa, "w") as archive:
                archive.writestr("Payload/KitapDuy.app/Info.plist", plist_bytes)
                archive.writestr("Payload/KitapDuy.app/tr.lproj/", b"")
                archive.writestr("Payload/KitapDuy.app/Frameworks/Other.framework/tr.lproj/Localizable.strings", b"bplist00")
            failed = subprocess.run([
                sys.executable, str(SCRIPT), str(missing_lproj_ipa),
                "--development-region", "tr",
                "--localization", "tr",
            ], capture_output=True, text=True)
            self.assertEqual(failed.returncode, 1)
            self.assertIn("tr.lproj", failed.stderr)

            empty_file_ipa = Path(directory) / "empty-lproj.ipa"
            with zipfile.ZipFile(empty_file_ipa, "w") as archive:
                archive.writestr("Payload/KitapDuy.app/Info.plist", plist_bytes)
                archive.writestr("Payload/KitapDuy.app/tr.lproj/InfoPlist.strings", b"")
            empty_failed = subprocess.run([
                sys.executable, str(SCRIPT), str(empty_file_ipa),
                "--development-region", "tr",
                "--localization", "tr",
                "--exact-localizations",
            ], capture_output=True, text=True)
            self.assertEqual(empty_failed.returncode, 1)
            self.assertIn("tr.lproj", empty_failed.stderr)

            stray_en_ipa = Path(directory) / "stray-en.ipa"
            with zipfile.ZipFile(stray_en_ipa, "w") as archive:
                archive.writestr("Payload/KitapDuy.app/Info.plist", plist_bytes)
                archive.writestr("Payload/KitapDuy.app/tr.lproj/InfoPlist.strings", b"bplist00")
                archive.writestr("Payload/KitapDuy.app/en.lproj/InfoPlist.strings", b"bplist00")
            stray_failed = subprocess.run([
                sys.executable, str(SCRIPT), str(stray_en_ipa),
                "--development-region", "tr",
                "--localization", "tr",
                "--exact-localizations",
            ], capture_output=True, text=True)
            self.assertEqual(stray_failed.returncode, 1)
            self.assertIn("en.lproj", stray_failed.stderr)

    def test_app_bundle_validation(self):
        with tempfile.TemporaryDirectory() as directory:
            app_dir = Path(directory) / "KitapDuy.app"
            (app_dir / "tr.lproj").mkdir(parents=True)
            (app_dir / "GoogleUserMessagingPlatform.bundle/en.lproj").mkdir(parents=True)
            (app_dir / "tr.lproj/InfoPlist.strings").write_bytes(b"bplist00")
            (app_dir / "GoogleUserMessagingPlatform.bundle/en.lproj/Localizable.strings").write_bytes(b"bplist00")
            (app_dir / "Info.plist").write_bytes(plistlib.dumps({
                validator.KEY: True,
                "CFBundleIdentifier": "com.semtinazizi.kitapduy",
                "CFBundleDevelopmentRegion": "tr",
                "CFBundleLocalizations": ["tr"],
            }, fmt=plistlib.FMT_BINARY))

            ok = subprocess.run([
                sys.executable, str(SCRIPT), str(app_dir),
                "--bundle-id", "com.semtinazizi.kitapduy",
                "--development-region", "tr",
                "--localization", "tr",
                "--exact-localizations",
            ], capture_output=True, text=True)
            self.assertEqual(ok.returncode, 0, ok.stderr)

            (app_dir / "en.lproj").mkdir()
            (app_dir / "en.lproj/Localizable.strings").write_bytes(b"bplist00")
            bad = subprocess.run([
                sys.executable, str(SCRIPT), str(app_dir),
                "--bundle-id", "com.semtinazizi.kitapduy",
                "--development-region", "tr",
                "--localization", "tr",
                "--exact-localizations",
            ], capture_output=True, text=True)
            self.assertEqual(bad.returncode, 1)
            self.assertIn("en.lproj", bad.stderr)

    def test_repo_info_plist_passes_localization_check(self):
        repo_plist = Path(__file__).resolve().parents[1] / "iosApp/iosApp/Info.plist"
        result = subprocess.run([
            sys.executable, str(SCRIPT), str(repo_plist),
            "--localization", "tr", "--exact-localizations",
        ], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
