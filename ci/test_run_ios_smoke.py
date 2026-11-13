from contextlib import nullcontext
import importlib.util
import json
from pathlib import Path
import plistlib
import tempfile
from types import SimpleNamespace
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import unittest
from unittest.mock import Mock, patch
import xml.etree.ElementTree as ET

from ios_smoke_fixture import USER_ID

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

    def test_ready_markers_finish_without_waiting_out_the_timeout(self):
        process = Mock(poll=Mock(return_value=None))
        sleep = Mock()
        smoke.monitor_app(process, self.console, 60, sleep=sleep)
        sleep.assert_not_called()

    def test_already_exited_process_fails_even_with_success_markers(self):
        process = Mock(poll=Mock(return_value=6))
        with self.assertRaisesRegex(RuntimeError, "App exited"):
            smoke.monitor_app(process, self.console, 60)

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
        with patch.object(smoke, "serve_fixture", return_value=nullcontext(("http://localhost:1234", []))), \
                patch.object(smoke.subprocess, "Popen", side_effect=OSError("launch failed")):
            result = runner.run_device(phone)
        self.assertFalse(result["passed"])
        self.assertIn("launch failed", result["error"])
        self.assertTrue((self.output / "iPhone/console.log").exists())
        cleanups = [call.args for call in runner.command.call_args_list if call.args[0] in ("shutdown", "delete")]
        self.assertEqual(cleanups, [("shutdown", created), ("delete", created)])

    def run_mock_journey(self, requests, test_status=0, screenshots=True, video=True, test_count=4):
        runner = self.make_runner()
        created = "13E121F2-2ACF-4F0B-8F30-85745C53CA4F"
        phone = dict(device("iPhone 17"), family="iPhone", runtime="runtime.iOS-27-2")

        def command(*args, **kwargs):
            if args[0] == "create":
                return created
            if args[0] == "io":
                Path(args[-1]).write_bytes(b"png")
            return ""

        def run_xcode(args, stdout, **kwargs):
            self.assertIn(f"platform=iOS Simulator,id={created}", args)
            if test_status:
                stdout.write("ReaderJourneyTests.swift:232: error: 15-feed-published: post missing\n")
            (self.output / "iPhone/journey.xcresult").mkdir()
            return SimpleNamespace(returncode=test_status)

        runner.command = Mock(side_effect=command)
        runner.collect_crashes = Mock()
        runner.start_recording = Mock(return_value=(Mock(), None))
        runner.stop_recording = Mock(return_value=video)
        runner.last_video_warning = "Video failed"
        with patch.object(smoke, "serve_fixture", return_value=nullcontext(("http://localhost:1234", requests))), \
                patch.object(smoke.subprocess, "Popen", return_value=Mock()), \
                patch.object(smoke, "monitor_app"), \
                patch.object(smoke.subprocess, "run", side_effect=run_xcode), \
                patch.object(smoke, "read_test_summary", return_value={"totalTestCount": test_count, "passedTests": test_count}), \
                patch.object(smoke, "export_evidence", return_value=["01-guest-entry.png"] if screenshots else []):
            return runner.run_device(phone)

    def test_xcuitest_failure_retains_video_and_failure_screenshot(self):
        result = self.run_mock_journey([], test_status=65)
        self.assertFalse(result["passed"])
        self.assertIn("XCUITest failed", result["error"])
        self.assertIn("15-feed-published: post missing", result["error"])
        self.assertIn("failure-screen.png", result["screenshots"])
        self.assertEqual(result["video"], "recording.mp4")
        self.assertTrue((self.output / "iPhone/journey.xcresult.zip").is_file())

    def test_local_ui_fallback_cannot_hide_missing_server_write(self):
        result = self.run_mock_journey(["guest_signup", "create_post"])
        self.assertFalse(result["passed"])
        self.assertIn("update_profile", result["error"])

    def test_success_exit_without_all_tests_is_a_failure(self):
        result = self.run_mock_journey(["guest_signup", "create_post", "update_profile"], test_count=1)
        self.assertFalse(result["passed"])
        self.assertIn("complete journey suite", result["error"])

    def test_missing_evidence_fails_successful_ui_run(self):
        result = self.run_mock_journey(["guest_signup", "create_post", "update_profile"], video=False)
        self.assertFalse(result["passed"])
        self.assertIn("video or milestone screenshots", result["error"])

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
        test_run = self.root / "test.xctestrun"
        test_run.touch()
        with patch.object(smoke, "SimulatorRunner", side_effect=RuntimeError("runtime unavailable")):
            code = smoke.main(["--app", str(self.app), "--test-run", str(test_run), "--output", str(self.output)])
        self.assertEqual(code, 1)
        root = ET.parse(self.output / "junit.xml").getroot()
        self.assertEqual(root.attrib["failures"], "1")


class FixtureTest(unittest.TestCase):
    def request_json(self, url, body=None, method=None, headers=None):
        data = None if body is None else json.dumps(body).encode()
        request = Request(url, data=data, method=method, headers={"Content-Type": "application/json", **(headers or {})})
        with urlopen(request, timeout=5) as response:
            payload = response.read()
            return json.loads(payload) if payload else None

    def test_guest_and_writes_are_isolated_and_audited(self):
        with smoke.serve_fixture() as (url, events):
            auth = self.request_json(url + "/auth/v1/signup", {})
            self.assertTrue(auth["user"]["is_anonymous"])
            users = url + "/rest/v1/users"
            posts = url + "/rest/v1/posts"
            self.request_json(users, [{"uid": USER_ID, "name": "Guest Reader", "username": "guest"}],
                              headers={"Prefer": "resolution=merge-duplicates"})
            self.assertEqual(self.request_json(users)[0]["name"], "Guest Reader")
            self.request_json(f"{users}?uid=eq.{USER_ID}", {"name": "Updated Reader"}, method="PATCH")
            self.request_json(posts, [{"comment": "Test", "user_id": USER_ID}])
            self.assertEqual(self.request_json(users)[0]["name"], "Updated Reader")
            self.assertEqual(self.request_json(posts)[0]["comment"], "Test")
            self.assertEqual(events, ["guest_signup", "update_profile", "create_post"])
        with smoke.serve_fixture() as (url, events):
            self.assertEqual(self.request_json(url + "/rest/v1/posts"), [])
            self.assertEqual(events, [])

    def test_invalid_post_rows_do_not_write_or_record_an_event(self):
        with smoke.serve_fixture() as (url, events):
            posts = url + "/rest/v1/posts"
            with self.assertRaises(HTTPError) as rejected:
                self.request_json(posts, [{"comment": "Do not save"}, "invalid row"])
            self.assertEqual(rejected.exception.code, 400)
            rejected.exception.close()
            self.assertEqual(self.request_json(posts), [])
            self.assertEqual(events, [])

    def test_empty_search_and_audio_range(self):
        with smoke.serve_fixture() as (url, _):
            self.assertEqual(self.request_json(url + "/rest/v1/books?or=(title.ilike.*notfound*)"), [])
            with urlopen(Request(url + "/audio.wav", headers={"Range": "bytes=0-15"}), timeout=5) as response:
                self.assertEqual(response.status, 206)
                self.assertEqual(len(response.read()), 16)
                self.assertTrue(response.headers["Content-Range"].startswith("bytes 0-15/"))


if __name__ == "__main__":
    unittest.main()
