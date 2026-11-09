import importlib.util
import json
from pathlib import Path
import plistlib
import tempfile
import unittest
from unittest.mock import Mock, patch
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location("ios_smoke", Path(__file__).with_name("run-ios-smoke.py"))
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


def device(name, available=True):
    return {"name": name, "isAvailable": available, "deviceTypeIdentifier": "type." + name}


class DeviceSelectionTest(unittest.TestCase):
    def test_newest_runtime_with_both_families_and_preferred_models(self):
        chosen = smoke.select_devices({
            "com.apple.CoreSimulator.SimRuntime.iOS-9-0": [device("iPhone 6"), device("iPad Air")],
            "com.apple.CoreSimulator.SimRuntime.iOS-27-2": [
                device("iPhone 16"), device("iPhone 17 Pro Max"),
                device("iPad Pro 13-inch"), device("iPad Air 11-inch (M3)"),
            ],
            "com.apple.CoreSimulator.SimRuntime.tvOS-28-0": [device("iPhone fake"), device("iPad fake")],
        })
        self.assertEqual([item["name"] for item in chosen], ["iPhone 17 Pro Max", "iPad Air 11-inch (M3)"])
        self.assertTrue(all(item["runtime"].endswith("iOS-27-2") for item in chosen))

    def test_incomplete_or_unavailable_new_runtime_uses_complete_older_runtime(self):
        chosen = smoke.select_devices({
            "runtime.iOS-27-2": [device("iPhone 17"), device("iPad Air", available=False)],
            "runtime.iOS-26-0": [device("iPhone 16"), device("iPad Pro")],
        })
        self.assertTrue(all(item["runtime"] == "runtime.iOS-26-0" for item in chosen))

    def test_missing_ipad_fails_instead_of_silently_testing_only_iphone(self):
        with self.assertRaisesRegex(RuntimeError, "both an available iPhone and iPad"):
            smoke.select_devices({"runtime.iOS-27-2": [device("iPhone 17")]})


class ProcessObservationTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.console = Path(self.temporary.name) / "console.log"
        self.console.write_text(smoke.UI_READY + "\n" + smoke.NETWORK_PASS, encoding="utf-8")

    def test_running_process_and_both_markers_are_required_for_success(self):
        process = Mock()
        process.poll.return_value = None
        clock = Mock(side_effect=[0, 0, 3])
        sleep = Mock()
        smoke.monitor_app(process, self.console, 3, clock=clock, sleep=sleep)
        sleep.assert_called_once_with(1)

    def test_delayed_crash_fails_even_after_success_markers(self):
        process = Mock()
        process.poll.side_effect = [None, 6]
        with self.assertRaisesRegex(RuntimeError, "process exited"):
            smoke.monitor_app(process, self.console, 60, clock=Mock(side_effect=[0, 1]), sleep=Mock())

    def test_alive_without_network_completion_is_failure(self):
        self.console.write_text(smoke.UI_READY, encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.NETWORK_PASS):
            smoke.monitor_app(process, self.console, 0)

    def test_alive_without_ui_appearance_is_failure(self):
        self.console.write_text(smoke.NETWORK_PASS, encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.UI_READY):
            smoke.monitor_app(process, self.console, 0)

    def test_failure_markers_override_an_earlier_success(self):
        process = Mock()
        process.poll.return_value = None
        for failure_marker in smoke.FAILURE_MARKERS:
            with self.subTest(marker=failure_marker):
                self.console.write_text(smoke.UI_READY + smoke.NETWORK_PASS + failure_marker, encoding="utf-8")
                with self.assertRaisesRegex(RuntimeError, "failure marker"):
                    smoke.monitor_app(process, self.console, 0)


class RunnerTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.app = self.root / "KitapDuy.app"
        self.app.mkdir()
        self.plist = {"CFBundleIdentifier": "com.semtinazizi.kitapduy", "CFBundleExecutable": "KitapDuy",
                      "DTPlatformName": "iphonesimulator"}
        self.write_plist()
        self.output = self.root / "evidence"

    def write_plist(self):
        with (self.app / "Info.plist").open("wb") as stream:
            plistlib.dump(self.plist, stream)

    def make_runner(self):
        return smoke.SimulatorRunner(self.app, self.output, 60, "https://example.com/")

    def test_device_ipa_cannot_be_mistaken_for_a_simulator_app(self):
        self.plist["DTPlatformName"] = "iphoneos"
        self.write_plist()
        with self.assertRaisesRegex(ValueError, "iphonesimulator"):
            self.make_runner()

    def test_second_family_is_still_attempted_after_first_failure(self):
        runner = self.make_runner()
        runner.command = Mock(return_value=json.dumps({"devices": {
            "runtime.iOS-27-2": [device("iPhone 17"), device("iPad Air")],
        }}))
        runner.run_device = Mock(side_effect=[{"passed": False}, {"passed": True}])
        results = runner.run()
        self.assertEqual(runner.run_device.call_count, 2)
        self.assertFalse(results[0]["passed"])

    def test_launch_failure_keeps_console_and_cleans_up_only_created_device(self):
        runner = self.make_runner()
        created = "11111111-2222-3333-4444-555555555555"
        runner.command = Mock(side_effect=lambda *args, **kwargs: created if args[0] == "create" else "")
        runner.collect_crashes = Mock()
        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")
        with patch.object(smoke.subprocess, "Popen", side_effect=OSError("launch failed")):
            result = runner.run_device(phone)
        self.assertFalse(result["passed"])
        self.assertIn("launch failed", result["error"])
        self.assertTrue((self.output / "iPhone/console.log").exists())
        cleanups = [call.args for call in runner.command.call_args_list if call.args[0] in ("shutdown", "delete")]
        self.assertEqual(cleanups, [("shutdown", created), ("delete", created)])

    def test_screenshot_failure_cannot_produce_a_passing_result(self):
        runner = self.make_runner()
        created = "11111111-2222-3333-4444-555555555555"

        def command(*args, **kwargs):
            if args[0] == "create":
                return created
            if args[0] == "io" and not kwargs.get("check") is False:
                raise RuntimeError("screenshot failed")
            return ""

        runner.command = Mock(side_effect=command)
        runner.collect_crashes = Mock()
        process = Mock()
        process.poll.return_value = None
        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")
        with patch.object(smoke.subprocess, "Popen", return_value=process), patch.object(smoke, "monitor_app"):
            result = runner.run_device(phone)
        self.assertFalse(result["passed"])
        self.assertIn("screenshot failed", result["error"])

    def test_success_requires_screenshot_and_sets_child_environment(self):
        runner = self.make_runner()
        created = "11111111-2222-3333-4444-555555555555"

        def command(*args, **kwargs):
            if args[0] == "create":
                return created
            if args[0] == "io":
                Path(args[-1]).write_bytes(b"\x89PNG\r\n\x1a\n")
            return ""

        runner.command = Mock(side_effect=command)
        runner.collect_crashes = Mock()
        process = Mock()
        process.poll.return_value = None
        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")
        with patch.object(smoke.subprocess, "Popen", return_value=process) as launch, \
                patch.object(smoke, "monitor_app") as monitor:
            result = runner.run_device(phone)
        self.assertTrue(result["passed"])
        self.assertTrue((self.output / "iPhone/screen.png").exists())
        self.assertEqual(launch.call_args.kwargs["env"]["SIMCTL_CHILD_KITAPDUY_CI_SMOKE_TEST"], "1")
        self.assertEqual(launch.call_args.kwargs["env"]["SIMCTL_CHILD_KITAPDUY_CI_HTTPS_URL"], "https://example.com/")
        self.assertEqual(monitor.call_args_list[0].args[-1], 60)
        self.assertEqual(monitor.call_args_list[1].args[-1], 0)

    def test_junit_and_json_preserve_the_failed_device(self):
        smoke.write_reports([
            {"name": "iPhone", "passed": False, "error": "native crash"},
            {"name": "iPad", "passed": True, "error": ""},
        ], self.output)
        root = ET.parse(self.output / "junit.xml").getroot()
        self.assertEqual(root.attrib["tests"], "2")
        self.assertEqual(root.attrib["failures"], "1")
        self.assertEqual(root.find("testcase/failure").attrib["message"], "native crash")
        self.assertEqual(len(json.loads((self.output / "summary.json").read_text(encoding="utf-8"))), 2)

    def test_setup_failure_is_reported_with_nonzero_exit_status(self):
        with patch.object(smoke, "SimulatorRunner", side_effect=RuntimeError("runtime unavailable")):
            code = smoke.main(["--app", str(self.app), "--output", str(self.output)])
        self.assertEqual(code, 1)
        root = ET.parse(self.output / "junit.xml").getroot()
        self.assertEqual(root.attrib["failures"], "1")


if __name__ == "__main__":
    unittest.main()
