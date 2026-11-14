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


if __name__ == "__main__":
    unittest.main()
