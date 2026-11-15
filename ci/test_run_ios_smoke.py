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

    def run_mock_journey(self, requests, test_status=0, screenshots=True, video=True, test_count=len(smoke.JOURNEYS)):
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
            for test in list(smoke.JOURNEYS)[:test_count]:
                outcome = "failed" if test_status and test == "test02BookPlayback" else "passed"
                stdout.write(f"Test Case '-[KitapDuyUITests.ReaderJourneyTests {test}]' {outcome} (1 second).\n")
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
        self.assertEqual(result["journeys"]["test02BookPlayback"], "failed")
        self.assertEqual(result["journeys"]["test04Library"], "passed")
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

    def test_gallery_keeps_library_and_playback_evidence_when_playback_fails(self):
        reference = self.output / "Android"
        reference.mkdir(parents=True)
        (reference / "27-library-favorites.png").touch()
        (reference / "source.txt").write_text("KitapDuy3 4.1.2", encoding="utf-8")
        smoke.write_reports([
            {"name": "Phone", "family": "iPhone", "passed": False, "error": "Playback stalled",
             "video": "recording.mp4", "screenshots": ["failure-06-book-playing.png", "27-library-favorites.png"],
             "journeys": {"test02BookPlayback": "failed", "test04Library": "passed"}},
            {"name": "Tablet", "family": "iPad", "passed": True,
             "screenshots": ["06-book-playing.png", "27-library-favorites.png"]},
        ], self.output)
        report = (self.output / "index.html").read_text(encoding="utf-8")
        self.assertIn('<video controls preload="none" src="iPhone/recording.mp4">', report)
        self.assertIn('iPhone/failure-06-book-playing.png', report)
        self.assertIn('iPad/06-book-playing.png', report)
        self.assertIn('iPhone/27-library-favorites.png', report)
        self.assertIn('Android/27-library-favorites.png', report)
        self.assertIn('KitapDuy3 4.1.2', report)
        self.assertIn('Kitaplık sekmeleri ve raflar <strong>Geçti</strong>', report)
        self.assertIn('Kitap oynatma, duraklatma ve atlama <strong>Hata</strong>', report)
        xml = ET.parse(self.output / "junit.xml").getroot()
        self.assertEqual(xml.attrib["failures"], "1")
        self.assertIsNone(xml.find("testcase[@name='Kitaplık sekmeleri ve raflar']/failure"))

    def test_setup_failure_is_reported_with_nonzero_exit_status(self):
        test_run = self.root / "test.xctestrun"
        test_run.touch()
        with patch.object(smoke, "SimulatorRunner", side_effect=RuntimeError("runtime unavailable")):
            code = smoke.main(["--app", str(self.app), "--test-run", str(test_run), "--output", str(self.output)])
        self.assertEqual(code, 1)
        root = ET.parse(self.output / "junit.xml").getroot()
        self.assertEqual(root.attrib["failures"], "1")

    def test_export_evidence_exports_alphanumeric_steps_audits_and_probes(self):
        device_dir = self.output / "iPhone"
        exported = device_dir / "xcode-attachments"
        exported.mkdir(parents=True)
        (exported / "a1.png").write_bytes(b"\x89PNG\r\n\x1a\n")
        (exported / "a2.png").write_bytes(b"\x89PNG\r\n\x1a\n")
        (exported / "audit1.txt").write_text("Kontrast uyarisi: metin soluk", encoding="utf-8")
        (exported / "probe1.json").write_text(
            json.dumps([
                {
                    "journey": "test09LiteraryAdvisorDeep",
                    "screen": "48-advisor-analysis-and-reading",
                    "theme": "Light",
                    "category": "Danisman",
                    "status": "INFO",
                    "summary": "Takip sorusu Sahaf tavsiyesi cevabina dustu",
                    "detail": "buildAdvisorReply icinde eslesme",
                },
                {
                    "journey": "test09LiteraryAdvisorDeep",
                    "screen": "50-advisor-custom-query-and-book",
                    "theme": "Light",
                    "category": "Danisman",
                    "status": "PASS",
                    "summary": "Onerilen kitap karta basinca BookDetail acildi",
                    "detail": "",
                },
            ]),
            encoding="utf-8",
        )
        manifest = [
            {
                "attachments": [
                    {"exportedFileName": "a1.png", "suggestedHumanReadableName": "24b-discover-categories_0_11111111-1111-1111-1111-111111111111.png"},
                    {"exportedFileName": "a2.png", "suggestedHumanReadableName": "46-advisor-welcome_0_22222222-2222-2222-2222-222222222222.png"},
                    {"exportedFileName": "audit1.txt", "suggestedHumanReadableName": "audit-Advisor_0_33333333-3333-3333-3333-333333333333.txt"},
                    {"exportedFileName": "probe1.json", "suggestedHumanReadableName": "probe-test09LiteraryAdvisorDeep_0_44444444-4444-4444-4444-444444444444.json"},
                ]
            }
        ]
        (exported / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")

        with patch.object(smoke.subprocess, "run"):
            screenshots = smoke.export_evidence(device_dir / "journey.xcresult", device_dir)

        self.assertEqual(screenshots, ["24b-discover-categories.png", "46-advisor-welcome.png"])
        self.assertTrue((device_dir / "24b-discover-categories.png").is_file())
        self.assertTrue((device_dir / "46-advisor-welcome.png").is_file())
        self.assertIn("Kontrast uyarisi", (device_dir / "accessibility.txt").read_text(encoding="utf-8"))
        probes = json.loads((device_dir / "probes.json").read_text(encoding="utf-8"))
        self.assertEqual(len(probes), 2)
        self.assertEqual(probes[0]["status"], "INFO")
        self.assertIn("Onerilen kitap karta basinca BookDetail acildi", (device_dir / "probes.txt").read_text(encoding="utf-8"))

    def test_write_reports_builds_inceleme_notu_and_copyable_panel(self):
        iphone_dir = self.output / "iPhone"
        ipad_dir = self.output / "iPad"
        iphone_dir.mkdir(parents=True)
        ipad_dir.mkdir(parents=True)
        (iphone_dir / "probes.json").write_text(
            json.dumps([
                {
                    "journey": "test09LiteraryAdvisorDeep",
                    "screen": "47-advisor-info-dialog",
                    "theme": "Light",
                    "category": "Danışman",
                    "status": "PASS",
                    "summary": "Hakkında diyaloğu tüm 3 giriş noktasından açıldı",
                    "detail": "",
                },
                {
                    "journey": "test04HomeCheck",
                    "screen": "43-author-detail",
                    "theme": "Light",
                    "category": "Metin",
                    "status": "FAIL",
                    "summary": "Eser sayısı boş görünüyor (' Eser')",
                    "detail": "AuthorDetailScreen satır 93",
                },
                {
                    "journey": "test10BookDetailFeaturesAndMiniPlayer",
                    "screen": "54-mini-player",
                    "theme": "Dark",
                    "category": "Dokunma Alanı",
                    "status": "WARN",
                    "summary": "Mini Player kapatma düğmesi 44pt altında",
                    "detail": "36x36",
                },
            ]),
            encoding="utf-8",
        )
        (iphone_dir / "probes.txt").write_text("probelist", encoding="utf-8")
        (iphone_dir / "accessibility.txt").write_text(
            "audit-Home\nContrast warning on Subtitle\n\naudit-Discover\nNo accessibility issues.\n\naudit-Advisor\nApple erişilebilirlik denetimi uyarı üretmedi.\n",
            encoding="utf-8",
        )
        (self.output / "observations.txt").write_text("Yazar ekranı Android ile karşılaştırıldı.", encoding="utf-8")

        results = [
            {
                "name": "iPhone 17 Pro Max",
                "family": "iPhone",
                "runtime": "iOS 26.2",
                "passed": True,
                "error": "",
                "seconds": 120.5,
                "xcuitest": {"totalTestCount": 14, "passedTests": 14, "failedTests": 0, "skippedTests": 0},
                "screenshots": ["01-guest-entry.png", "46-advisor-welcome.png"],
                "video": "recording.mp4",
                "journeys": {test: "passed" for test in smoke.JOURNEYS},
                "fixture_requests": ["guest_signup", "create_comment", "follow_user", "donate_askida"],
            },
            {
                "name": "iPad Air 11-inch (M3)",
                "family": "iPad",
                "runtime": "iOS 26.2",
                "passed": True,
                "error": "",
                "seconds": 130.0,
                "xcuitest": {"totalTestCount": 14, "passedTests": 14, "failedTests": 0, "skippedTests": 0},
                "screenshots": ["01-guest-entry.png", "46-advisor-welcome.png"],
                "video": "recording.mp4",
                "journeys": {test: "passed" for test in smoke.JOURNEYS},
                "fixture_requests": ["guest_signup", "create_post"],
            },
        ]

        smoke.write_reports(results, self.output)

        note_md = (self.output / "inceleme-notu.md").read_text(encoding="utf-8")
        note_txt = (self.output / "inceleme-notu.txt").read_text(encoding="utf-8")
        gallery = (self.output / "index.html").read_text(encoding="utf-8")
        self.assertIn("Edebi Danışman (`LiteraryAdvisorScreen`) Özel İnceleme Bölümü", note_md)
        self.assertIn("Eser sayısı boş görünüyor", note_md)
        self.assertIn("Mini Player kapatma düğmesi 44pt altında", note_md)
        self.assertIn("Contrast warning on Subtitle", note_md)
        self.assertNotIn("No accessibility issues.", note_md)
        self.assertNotIn("Apple erişilebilirlik denetimi uyarı üretmedi.", note_md)
        self.assertIn("Erişilebilirlik bulgusu: `1`", note_md)
        self.assertIn("**Doğrulandı** `create_comment`", note_md)
        self.assertIn("EntertainmentTab", note_md)
        self.assertIn("Yazar ekranı Android ile karşılaştırıldı.", note_md)
        self.assertIn("DETAYLI", note_txt.upper())
        self.assertIn("Notu Panoya Kopyala", gallery)
        self.assertIn("inceleme-notu.md", gallery)
        self.assertIn("inceleme-notu.txt", gallery)
        self.assertIn("iPhone/probes.txt", gallery)
        self.assertIn("navigator.clipboard", gallery)


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
            book = self.request_json(url + "/rest/v1/books")[0]
            audio_url = book["audio_url"]
            with urlopen(url + "/podcast.xml", timeout=5) as response:
                self.assertIn(f'{audio_url}?episode=1" type="audio/mpeg"'.encode(), response.read())
            with urlopen(Request(audio_url, method="HEAD"), timeout=5) as response:
                self.assertEqual(response.headers["Content-Type"], "audio/mpeg")
                self.assertEqual(response.headers["Accept-Ranges"], "bytes")
                audio_size = int(response.headers["Content-Length"])
            with urlopen(Request(audio_url, headers={"Range": "bytes=0-15"}), timeout=5) as response:
                self.assertEqual(response.status, 206)
                self.assertEqual(len(response.read()), 16)
                self.assertEqual(response.headers["Content-Range"], f"bytes 0-15/{audio_size}")

    def test_enriched_catalog_serves_all_categories_with_valid_media_urls(self):
        app_icon = (
            Path(__file__).resolve().parents[1]
            / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon_1024.png"
        ).read_bytes()
        expected_categories = {
            "Roman",
            "Dünya Edebiyatı",
            "Türk Edebiyatı",
            "Polisiye",
            "Kişisel Gelişim",
            "Çocuk Masalları",
            "İngilizce Kitaplar",
            "Klasik",
            "Tarih",
            "Felsefe",
            "Şiir",
            "Bilim Kurgu",
        }
        with smoke.serve_fixture() as (url, _):
            books = self.request_json(url + "/rest/v1/books")
            self.assertGreaterEqual(len(books), 368)
            self.assertEqual(books[0]["id"], "local_ci_book")
            self.assertEqual(books[0]["title"], "KitapDuy Deneme Kitabı")
            category_and_tag_counts = {}
            for book in books:
                all_labels = {book["category"], *book.get("tags", [])}
                for label in all_labels:
                    if label:
                        category_and_tag_counts[label] = category_and_tag_counts.get(label, 0) + 1
                self.assertIn("/covers/", book["cover_url"])
                self.assertTrue(book["cover_url"].endswith(".jpg"))
                self.assertTrue(book["audio_url"].endswith("/audio.mp3"))
            for expected in expected_categories:
                self.assertGreaterEqual(
                    category_and_tag_counts.get(expected, 0),
                    1,
                    f"{expected} should appear in catalog categories/tags",
                )
            self.assertGreaterEqual(category_and_tag_counts.get("Roman", 0), 18)
            self.assertGreaterEqual(category_and_tag_counts.get("Polisiye", 0), 28)
            with urlopen(books[1]["cover_url"], timeout=5) as cover_resp:
                cover_bytes = cover_resp.read()
                self.assertEqual(cover_resp.status, 200)
                self.assertEqual(cover_resp.headers["Content-Type"], "image/jpeg")
                self.assertNotEqual(cover_bytes, app_icon)
                self.assertGreater(len(cover_bytes), 100)
            podcasts = self.request_json(url + "/rest/v1/podcasts")
            self.assertGreaterEqual(len(podcasts), 3)
            for podcast in podcasts:
                self.assertIn("/covers/", podcast["image_url"])
                self.assertTrue(podcast["image_url"].endswith(".jpg"))
                with urlopen(podcast["image_url"], timeout=5) as pod_resp:
                    self.assertEqual(pod_resp.status, 200)
                    self.assertEqual(pod_resp.headers["Content-Type"], "image/jpeg")
                    self.assertNotEqual(pod_resp.read(), app_icon)
            with self.assertRaises(HTTPError) as rejected_cover:
                urlopen(url + "/covers/../test_run_ios_smoke.py", timeout=5)
            self.assertEqual(rejected_cover.exception.code, 404)
            rejected_cover.exception.close()
            kizil_matches = self.request_json(url + "/rest/v1/books?or=(title.ilike.*K%C4%B1z%C4%B1l%20Dosya*)")
            self.assertIn("0mVxipNVFmBxtJNs9O0J", [b["id"] for b in kizil_matches])
            podcast_matches = self.request_json(url + "/rest/v1/podcasts?or=(name.ilike.*111*)")
            self.assertEqual([p["id"] for p in podcast_matches], ["podcast_111hz"])

    def test_askida_and_quiz_leaderboard_fixture_routes(self):
        with smoke.serve_fixture() as (url, _):
            askida = self.request_json(url + "/rest/v1/askida_kitap?status=eq.AVAILABLE")
            self.assertGreaterEqual(len(askida), 3)
            self.assertTrue(any("Kürk Mantolu Madonna" in b["book_name"] for b in askida))
            self.request_json(
                url + "/rest/v1/askida_kitap",
                [{"id": "new_askida_1", "book_name": "Simyacı", "donor_id": USER_ID, "city": "Bursa", "status": "AVAILABLE"}],
            )
            after_donate = self.request_json(url + "/rest/v1/askida_kitap?status=eq.AVAILABLE")
            self.assertTrue(any(b["id"] == "new_askida_1" for b in after_donate))
            self.request_json(
                url + "/rest/v1/askida_kitap?id=eq.new_askida_1",
                {"status": "CLAIMED", "claimer_id": USER_ID},
                method="PATCH",
            )
            claimed = self.request_json(f"{url}/rest/v1/askida_kitap?or=(donor_id.eq.{USER_ID},claimer_id.eq.{USER_ID})")
            self.assertTrue(any(b["id"] == "new_askida_1" and b["status"] == "CLAIMED" for b in claimed))

            leaderboard = self.request_json(url + "/rest/v1/quiz_leaderboard")
            self.assertGreaterEqual(len(leaderboard), 5)
            self.request_json(
                url + "/rest/v1/quiz_leaderboard",
                [{"user_id": USER_ID, "user_name": "Deneme Okuru", "total_score": 5000}],
            )
            updated_board = self.request_json(url + "/rest/v1/quiz_leaderboard")
            self.assertEqual(updated_board[0]["user_id"], USER_ID)
            self.assertEqual(updated_board[0]["total_score"], 5000)

    def test_extended_social_library_notifications_and_filters(self):
        with smoke.serve_fixture() as (url, events):
            # users[0] must remain current profile, while community_users are searchable.
            all_users = self.request_json(url + "/rest/v1/users")
            self.assertEqual(all_users[0]["uid"], USER_ID)
            elif_hits = self.request_json(url + "/rest/v1/users?or=(name.ilike.*Elif*,username.ilike.*Elif*)&limit=10")
            self.assertEqual([u["uid"] for u in elif_hits], ["community_donor_1"])
            self.assertEqual(elif_hits[0]["username"], "elif_okur")

            # HEAD count=exact + Prefer: return=representation + DELETE on user_bookshelves and favorites.
            head_req = Request(
                f"{url}/rest/v1/user_bookshelves?user_id=eq.{USER_ID}",
                method="HEAD",
                headers={"Prefer": "count=exact"},
            )
            with urlopen(head_req, timeout=5) as head_resp:
                self.assertEqual(head_resp.headers["Content-Range"], "*/0")
            created_shelf = self.request_json(
                url + "/rest/v1/user_bookshelves",
                [{"user_id": USER_ID, "book_id": "local_ci_book", "shelf_type": "READ"}],
                headers={"Prefer": "return=representation"},
            )
            self.assertEqual(len(created_shelf), 1)
            with urlopen(head_req, timeout=5) as head_after:
                self.assertEqual(head_after.headers["Content-Range"], "0-0/1")
            self.request_json(
                f"{url}/rest/v1/user_bookshelves?user_id=eq.{USER_ID}&book_id=eq.local_ci_book",
                method="DELETE",
            )
            self.assertEqual(self.request_json(f"{url}/rest/v1/user_bookshelves?user_id=eq.{USER_ID}"), [])

            fav_rows = self.request_json(
                url + "/rest/v1/favorites?select=*",
                [{"user_id": USER_ID, "book_id": "local_ci_book"}],
            )
            self.assertEqual(len(fav_rows), 1)
            self.request_json(f"{url}/rest/v1/favorites?user_id=eq.{USER_ID}&book_id=eq.local_ci_book", method="DELETE")
            self.assertEqual(self.request_json(f"{url}/rest/v1/favorites?user_id=eq.{USER_ID}"), [])

            # Posts & comments CRUD + poll PATCH + followers + notifications mark-read.
            created_post = self.request_json(
                url + "/rest/v1/posts",
                [{"user_id": USER_ID, "content": "Anket", "category": "Sohbet", "poll_options": ["A", "B"]}],
                headers={"Prefer": "return=representation"},
            )[0]
            post_id = created_post["id"]
            self.request_json(
                f"{url}/rest/v1/posts?id=eq.{post_id}",
                {"poll_votes": {"0": 1}, "poll_voters": [USER_ID]},
                method="PATCH",
            )
            created_comment = self.request_json(
                url + "/rest/v1/feed_comments?select=*",
                [{"post_id": post_id, "user_id": USER_ID, "comment_text": "İlk yorum"}],
            )[0]
            comment_id = created_comment["id"]
            self.request_json(
                f"{url}/rest/v1/feed_comments?id=eq.{comment_id}",
                {"comment_text": "Güncellenen yorum"},
                method="PATCH",
            )
            comments = self.request_json(f"{url}/rest/v1/feed_comments?post_id=eq.{post_id}")
            self.assertEqual(comments[0]["comment_text"], "Güncellenen yorum")
            self.request_json(f"{url}/rest/v1/feed_comments?id=eq.{comment_id}", method="DELETE")
            self.assertEqual(self.request_json(f"{url}/rest/v1/feed_comments?post_id=eq.{post_id}"), [])
            self.request_json(f"{url}/rest/v1/posts?id=eq.{post_id}", method="DELETE")

            # Quiz Arena multiplayer match room creation, query, update & delete.
            self.assertEqual(self.request_json(url + "/rest/v1/quiz_matches?status=eq.WAITING&limit=1"), [])
            created_match = self.request_json(
                url + "/rest/v1/quiz_matches?select=*",
                [{"match_id": "", "player1_id": USER_ID, "player1_name": "Deneme Okuru", "status": "WAITING"}],
                headers={"Accept": "application/vnd.pgrst.object+json"},
            )
            match_id = created_match["match_id"]
            self.assertTrue(match_id)
            self.request_json(
                f"{url}/rest/v1/quiz_matches?match_id=eq.{match_id}",
                {"status": "PLAYING", "player2_id": "community_donor_1"},
                method="PATCH",
            )
            playing = self.request_json(f"{url}/rest/v1/quiz_matches?match_id=eq.{match_id}")
            self.assertEqual(playing[0]["status"], "PLAYING")
            self.request_json(f"{url}/rest/v1/quiz_matches?match_id=eq.{match_id}", method="DELETE")
            self.assertEqual(self.request_json(f"{url}/rest/v1/quiz_matches?match_id=eq.{match_id}"), [])

            self.request_json(
                url + "/rest/v1/followers",
                [{"follower_id": USER_ID, "following_id": "community_donor_1"}],
            )
            self.request_json(
                f"{url}/rest/v1/followers?follower_id=eq.{USER_ID}&following_id=eq.community_donor_1",
                method="DELETE",
            )

            unread = self.request_json(url + "/rest/v1/notifications?is_read=eq.false")
            self.assertGreaterEqual(len(unread), 1)
            ids_csv = ",".join(str(item["id"]) for item in unread)
            self.request_json(
                f"{url}/rest/v1/notifications?id=in.({ids_csv})",
                {"is_read": True},
                method="PATCH",
            )
            self.assertEqual(self.request_json(url + "/rest/v1/notifications?is_read=eq.false"), [])

            for expected_event in (
                "add_shelf", "create_post", "update_post", "create_comment",
                "delete_comment", "delete_post", "follow_user", "unfollow_user",
                "mark_notifications_read", "create_quiz_match",
            ):
                self.assertIn(expected_event, events)

    def test_xcuitest_steps_and_journeys_match_screen_groups(self):
        swift_path = Path(__file__).resolve().parents[1] / "iosApp/KitapDuyUITests/ReaderJourneyTests.swift"
        swift_text = swift_path.read_text(encoding="utf-8")
        for journey_name in smoke.JOURNEYS:
            self.assertIn(f"func {journey_name}()", swift_text)
        for _, _, screens in smoke.SCREEN_GROUPS:
            for step_name, _ in screens:
                self.assertIn(f'step("{step_name}")', swift_text)


if __name__ == "__main__":
    unittest.main()
