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
        self.console.write_text("\n".join(smoke.REQUIRED_MARKERS), encoding="utf-8")

    def test_running_process_and_all_markers_are_required_for_success(self):
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
        self.console.write_text("\n".join(m for m in smoke.REQUIRED_MARKERS if m != smoke.NETWORK_PASS), encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.NETWORK_PASS):
            smoke.monitor_app(process, self.console, 0)

    def test_alive_without_ui_appearance_is_failure(self):
        self.console.write_text("\n".join(m for m in smoke.REQUIRED_MARKERS if m != smoke.UI_READY), encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.UI_READY):
            smoke.monitor_app(process, self.console, 0)

    def test_alive_without_guest_login_is_failure(self):
        self.console.write_text("\n".join(m for m in smoke.REQUIRED_MARKERS if m != smoke.GUEST_LOGIN_PASS), encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.GUEST_LOGIN_PASS):
            smoke.monitor_app(process, self.console, 0)

    def test_alive_without_audio_playing_is_failure(self):
        self.console.write_text("\n".join(m for m in smoke.REQUIRED_MARKERS if m != smoke.AUDIO_PLAYING), encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.AUDIO_PLAYING):
            smoke.monitor_app(process, self.console, 0)

    def test_alive_without_simulation_pass_is_failure(self):
        self.console.write_text("\n".join(m for m in smoke.REQUIRED_MARKERS if m != smoke.SIMULATION_PASS), encoding="utf-8")
        process = Mock()
        process.poll.return_value = None
        with self.assertRaisesRegex(RuntimeError, smoke.SIMULATION_PASS):
            smoke.monitor_app(process, self.console, 0)

    def test_milestone_screenshots_are_triggered_when_markers_appear(self):
        process = Mock()
        process.poll.return_value = None
        called_markers = []
        self.console.write_text("\n".join(smoke.REQUIRED_MARKERS), encoding="utf-8")
        smoke.monitor_app(process, self.console, 0, on_marker=lambda m: called_markers.append(m))
        self.assertIn(smoke.HOME_LOADED, called_markers)
        self.assertIn(smoke.AUDIO_PLAYING, called_markers)
        self.assertIn(smoke.TAB_SEARCH_PASS, called_markers)
        self.assertIn(smoke.TAB_FEED_PASS, called_markers)
        self.assertIn(smoke.TAB_PROFILE_PASS, called_markers)

    def test_failure_markers_override_an_earlier_success(self):
        process = Mock()
        process.poll.return_value = None
        for failure_marker in smoke.FAILURE_MARKERS:
            with self.subTest(marker=failure_marker):
                self.console.write_text("\n".join(smoke.REQUIRED_MARKERS) + "\n" + failure_marker, encoding="utf-8")
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
        app_launch_call = launch.call_args_list[0]
        self.assertIn("--console-pty", app_launch_call.args[0])
        self.assertEqual(app_launch_call.kwargs["env"]["SIMCTL_CHILD_KITAPDUY_CI_SMOKE_TEST"], "1")
        self.assertEqual(app_launch_call.kwargs["env"]["SIMCTL_CHILD_KITAPDUY_CI_HTTPS_URL"], "https://example.com/")
        bootstatus_calls = [call.args for call in runner.command.call_args_list if len(call.args) > 0 and call.args[0] == "bootstatus"]
        self.assertTrue(len(bootstatus_calls) > 0)
        self.assertNotIn("-b", bootstatus_calls[0])
        video_call = launch.call_args_list[1]
        self.assertEqual(video_call.args[0][:5], ["xcrun", "simctl", "io", created, "recordVideo"])
        self.assertIn("--codec=h264", video_call.args[0])
        self.assertEqual(monitor.call_args_list[0].args[-1], 60)
        self.assertEqual(monitor.call_args_list[1].args[-1], 0)

    def test_milestone_screenshots_are_recorded_in_device_result(self):
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

        def fake_monitor(proc, console, duration, on_marker=None):
            if on_marker:
                for marker in smoke.MILESTONE_SCREENSHOTS:
                    on_marker(marker)

        with patch.object(smoke.subprocess, "Popen", return_value=process), \
                patch.object(smoke, "monitor_app", side_effect=fake_monitor):
            result = runner.run_device(phone)

        self.assertTrue(result["passed"])
        self.assertIn("01_home.png", result["screenshots"])
        self.assertIn("02_player.png", result["screenshots"])
        self.assertIn("03_search.png", result["screenshots"])
        self.assertIn("04_feed.png", result["screenshots"])
        self.assertIn("05_profile.png", result["screenshots"])
        self.assertIn("screen.png", result["screenshots"])

    def test_video_recording_lifecycle_and_recorded_artifact(self):
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

        app_process = Mock()
        app_process.poll.return_value = None

        video_process = Mock()
        video_process.poll.return_value = None

        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")

        def fake_popen(cmd, *args, **kwargs):
            if "recordVideo" in cmd:
                video_file = Path(cmd[-1])
                video_file.write_bytes(b"\x00\x00\x00 ftypmp42")
                return video_process
            return app_process

        with patch.object(smoke.subprocess, "Popen", side_effect=fake_popen), \
                patch.object(smoke, "monitor_app"):
            result = runner.run_device(phone)

        self.assertTrue(result["passed"])
        video_process.send_signal.assert_called_once_with(smoke.signal.SIGINT)
        video_process.wait.assert_called_once_with(timeout=15)
        self.assertEqual(result.get("video"), "recording.mp4")
        self.assertTrue((self.output / "iPhone/recording.mp4").exists())

    def test_video_recording_failure_does_not_fail_smoke_test(self):
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

        app_process = Mock()
        app_process.poll.return_value = None

        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")

        def fake_popen(cmd, *args, **kwargs):
            if "recordVideo" in cmd:
                raise OSError("simctl recordVideo not supported")
            return app_process

        with patch.object(smoke.subprocess, "Popen", side_effect=fake_popen), \
                patch.object(smoke, "monitor_app"):
            result = runner.run_device(phone)

        self.assertTrue(result["passed"])
        self.assertIn("video_warning", result)
        self.assertNotIn("video", result)

    def test_video_recording_timeout_falls_back_to_terminate(self):
        runner = self.make_runner()
        video_path = self.output / "recording.mp4"
        video_path.write_bytes(b"\x00\x00\x00 ftypmp42")

        video_process = Mock()
        video_process.poll.return_value = None
        video_process.wait.side_effect = [smoke.subprocess.TimeoutExpired(cmd="recordVideo", timeout=15), None]

        success = runner.stop_recording(video_process, video_path)
        video_process.send_signal.assert_called_once_with(smoke.signal.SIGINT)
        video_process.terminate.assert_called_once()
        self.assertTrue(success)

    def test_video_process_is_stopped_in_cleanup_on_app_failure(self):
        runner = self.make_runner()
        created = "11111111-2222-3333-4444-555555555555"
        runner.command = Mock(side_effect=lambda *args, **kwargs: created if args[0] == "create" else "")
        runner.collect_crashes = Mock()

        app_process = Mock()
        app_process.poll.return_value = None

        video_process = Mock()
        video_process.poll.return_value = None

        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")

        def fake_popen(cmd, *args, **kwargs):
            if "recordVideo" in cmd:
                return video_process
            return app_process

        with patch.object(smoke.subprocess, "Popen", side_effect=fake_popen), \
                patch.object(smoke, "monitor_app", side_effect=RuntimeError("App crashed")):
            result = runner.run_device(phone)

        self.assertFalse(result["passed"])
        self.assertIn("App crashed", result["error"])
        video_process.send_signal.assert_called_once_with(smoke.signal.SIGINT)
        video_process.wait.assert_called()

    def test_video_recording_fallback_to_kill_on_hung_process(self):
        runner = self.make_runner()
        video_path = self.output / "recording.mp4"
        video_path.write_bytes(b"\x00\x00\x00 ftypmp42")

        video_process = Mock()
        video_process.poll.return_value = None
        video_process.wait.side_effect = [
            smoke.subprocess.TimeoutExpired(cmd="recordVideo", timeout=15),
            smoke.subprocess.TimeoutExpired(cmd="recordVideo", timeout=5),
            None,
        ]

        success = runner.stop_recording(video_process, video_path)
        video_process.send_signal.assert_called_once_with(smoke.signal.SIGINT)
        video_process.terminate.assert_called_once()
        video_process.kill.assert_called_once()
        self.assertTrue(success)

    def test_video_recording_premature_exit_reports_failure_and_log(self):
        runner = self.make_runner()
        video_path = self.output / "recording.mp4"
        video_path.write_bytes(b"\x00\x00\x00 ftypmp42")
        log_path = self.output / "recordVideo.log"
        log_path.write_text("simctl error: simulator display not ready", encoding="utf-8")

        video_process = Mock()
        video_process.poll.return_value = 1
        video_process.returncode = 1

        success = runner.stop_recording(video_process, video_path)
        self.assertFalse(success)
        video_process.send_signal.assert_not_called()
        self.assertIn("exited prematurely with code 1", runner.last_video_warning)
        self.assertIn("simulator display not ready", runner.last_video_warning)

    def test_video_recording_empty_file_reports_warning_with_log_output(self):
        runner = self.make_runner()
        video_path = self.output / "recording.mp4"
        video_path.write_bytes(b"")
        log_path = self.output / "recordVideo.log"
        log_path.write_text("simctl: codec h264 not supported", encoding="utf-8")

        video_process = Mock()
        video_process.poll.return_value = None

        success = runner.stop_recording(video_process, video_path)
        self.assertFalse(success)
        self.assertIn("Video recording file was not generated or empty", runner.last_video_warning)
        self.assertIn("codec h264 not supported", runner.last_video_warning)

    def test_video_recording_cleanup_on_app_failure_reports_video_warning_if_empty(self):
        runner = self.make_runner()
        created = "11111111-2222-3333-4444-555555555555"
        runner.command = Mock(side_effect=lambda *args, **kwargs: created if args[0] == "create" else "")
        runner.collect_crashes = Mock()

        app_process = Mock()
        app_process.poll.return_value = None

        video_process = Mock()
        video_process.poll.return_value = None

        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")

        def fake_popen(cmd, *args, **kwargs):
            if "recordVideo" in cmd:
                return video_process
            return app_process

        with patch.object(smoke.subprocess, "Popen", side_effect=fake_popen), \
                patch.object(smoke, "monitor_app", side_effect=RuntimeError("App crashed")):
            result = runner.run_device(phone)

        self.assertFalse(result["passed"])
        self.assertNotIn("video", result)
        self.assertIn("video_warning", result)
        video_process.send_signal.assert_called_once_with(smoke.signal.SIGINT)

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
