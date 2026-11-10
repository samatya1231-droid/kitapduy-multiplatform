"""Launch the real simulator app on fresh iPhone/iPad devices and retain evidence."""

import argparse
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import signal
import subprocess
import sys
import time
from urllib.parse import urlsplit
import uuid
import xml.etree.ElementTree as ET

UI_READY = "KITAPDUY_CI_UI_APPEARED"
NETWORK_PASS = "KITAPDUY_CI_NETWORK_PASS"
GUEST_LOGIN_PASS = "KITAPDUY_CI_GUEST_LOGIN_PASS"
HOME_LOADED = "KITAPDUY_CI_HOME_LOADED"
AUDIO_PLAYING = "KITAPDUY_CI_AUDIO_PLAYING"
TAB_SEARCH_PASS = "KITAPDUY_CI_TAB_SEARCH_PASS"
TAB_FEED_PASS = "KITAPDUY_CI_TAB_FEED_PASS"
TAB_PROFILE_PASS = "KITAPDUY_CI_TAB_PROFILE_PASS"
SIMULATION_PASS = "KITAPDUY_CI_USER_SIMULATION_PASS"

REQUIRED_MARKERS = (
    UI_READY,
    NETWORK_PASS,
    GUEST_LOGIN_PASS,
    HOME_LOADED,
    AUDIO_PLAYING,
    TAB_SEARCH_PASS,
    TAB_FEED_PASS,
    TAB_PROFILE_PASS,
    SIMULATION_PASS,
)

MILESTONE_SCREENSHOTS = {
    HOME_LOADED: "01_home.png",
    AUDIO_PLAYING: "02_player.png",
    TAB_SEARCH_PASS: "03_search.png",
    TAB_FEED_PASS: "04_feed.png",
    TAB_PROFILE_PASS: "05_profile.png",
}

FAILURE_MARKERS = (
    "KITAPDUY_CI_NETWORK_FAIL",
    "KITAPDUY_CI_SIMULATION_FAIL",
    "IrLinkageError",
    "Unhandled Kotlin exception caught by hook",
    "Uncaught Kotlin exception",
)


def select_devices(devices):
    """Use the newest installed iOS runtime that has both available device families."""
    runtimes = sorted(
        (key for key in devices if re.search(r"\.iOS-\d", key)),
        key=lambda key: tuple(int(part) for part in re.findall(r"\d+", key)),
        reverse=True,
    )
    for runtime in runtimes:
        available = [device for device in devices[runtime]
                     if device.get("isAvailable") and device.get("deviceTypeIdentifier")]
        selected = []
        for family, preferred in (("iPhone", "iPhone 17 Pro Max"), ("iPad", "iPad Air 11-inch (M3)")):
            matches = [device for device in available if device["name"].startswith(family)]
            if not matches:
                break
            device = next((device for device in matches if device["name"] == preferred), matches[0])
            selected.append(dict(device, runtime=runtime, family=family))
        if len(selected) == 2:
            return selected
    raise RuntimeError("No installed iOS runtime has both an available iPhone and iPad. Inspect devices.json.")


def monitor_app(process, console, duration, on_marker=None, clock=time.monotonic, sleep=time.sleep):
    deadline = clock() + duration
    handled_markers = set()
    while True:
        if process.poll() is not None:
            raise RuntimeError("The attached app process exited before the observation period ended.")
        text = console.read_text(encoding="utf-8", errors="replace")
        for marker in FAILURE_MARKERS:
            if marker in text:
                raise RuntimeError(f"App log contains a failure marker: {marker}")
        if on_marker:
            for marker in MILESTONE_SCREENSHOTS:
                if marker in text and marker not in handled_markers:
                    handled_markers.add(marker)
                    try:
                        on_marker(marker)
                    except Exception:
                        pass
        if clock() >= deadline:
            missing = [marker for marker in REQUIRED_MARKERS if marker not in text]
            if missing:
                raise RuntimeError("Missing app evidence: " + ", ".join(missing))
            return
        sleep(1)


def write_reports(results, output):
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    suite = ET.Element("testsuite", name="iOS simulator launch and HTTPS", tests=str(len(results)),
                       failures=str(sum(not result["passed"] for result in results)))
    for result in results:
        case = ET.SubElement(suite, "testcase", classname="KitapDuy.SimulatorSmoke",
                             name=result["name"], time=str(result.get("seconds", 0)))
        if not result["passed"]:
            ET.SubElement(case, "failure", message=result["error"]).text = result["error"]
        ET.SubElement(case, "system-out").text = json.dumps(result, indent=2)
    ET.ElementTree(suite).write(output / "junit.xml", encoding="utf-8", xml_declaration=True)


class SimulatorRunner:
    def __init__(self, app, output, duration, url):
        self.app, self.output, self.duration, self.url = app, output, duration, url
        with (app / "Info.plist").open("rb") as stream:
            plist = plistlib.load(stream)
        self.bundle = plist["CFBundleIdentifier"]
        self.executable = plist["CFBundleExecutable"]
        if plist.get("DTPlatformName") != "iphonesimulator":
            raise ValueError("The app must be built for iphonesimulator, not an IPA/device build.")
        if self.bundle != "com.semtinazizi.kitapduy":
            raise ValueError("Unexpected application bundle identifier.")
        self.output.mkdir(parents=True, exist_ok=True)

    def command(self, *args, timeout=120, check=True):
        completed = subprocess.run(["xcrun", "simctl", *args], text=True, encoding="utf-8",
                                   errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   timeout=timeout)
        with (self.output / "runner.log").open("a", encoding="utf-8") as log:
            log.write(f"simctl {' '.join(args)}\n{completed.stdout}{completed.stderr}\n")
        if check and completed.returncode:
            raise RuntimeError(f"simctl {args[0]} failed: {completed.stderr.strip()}")
        return completed.stdout.strip()

    def collect_crashes(self, udid, directory, started):
        locations = [Path.home() / "Library/Logs/DiagnosticReports",
                     Path.home() / "Library/Developer/CoreSimulator/Devices" / udid / "data/Library/Logs/CrashReporter"]
        for location in locations:
            if not location.exists():
                continue
            for path in location.rglob(f"{self.executable}*"):
                if path.is_file() and path.suffix in (".ips", ".crash") and path.stat().st_mtime >= started:
                    shutil.copy2(path, directory / path.name)

    def start_recording(self, udid, video_path):
        """Start recording simulator video to an MP4 file in the background."""
        log_stream = None
        try:
            if video_path.exists():
                try:
                    video_path.unlink()
                except Exception:
                    pass
            log_path = video_path.with_name("recordVideo.log")
            log_stream = log_path.open("w", encoding="utf-8")
            process = subprocess.Popen(
                ["xcrun", "simctl", "io", udid, "recordVideo", "--codec=h264", str(video_path)],
                stdout=log_stream,
                stderr=subprocess.STDOUT,
            )
            return process, log_stream
        except Exception as error:
            if log_stream:
                try:
                    log_stream.close()
                except Exception:
                    pass
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Failed to start video recording on {udid}: {error}\n")
            return None, None

    def stop_recording(self, process, video_path):
        """Cleanly stop the video recording subprocess so the MP4 trailer is written."""
        if not process:
            return False
        try:
            if process.poll() is None:
                try:
                    process.send_signal(signal.SIGINT)
                except Exception:
                    try:
                        process.terminate()
                    except Exception:
                        pass
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    try:
                        process.terminate()
                        process.wait(timeout=5)
                    except Exception:
                        try:
                            process.kill()
                            process.wait(timeout=5)
                        except Exception:
                            pass
            return video_path.exists() and video_path.stat().st_size > 0
        except Exception as error:
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Error stopping video recording: {error}\n")
            return False

    def run_device(self, device):
        directory = self.output / device["family"]
        directory.mkdir(parents=True, exist_ok=True)
        result = {"name": device["name"], "family": device["family"], "runtime": device["runtime"],
                  "passed": False, "error": "", "artifacts": str(directory)}
        started = time.time()
        udid = None
        process = None
        console_stream = None
        video_process = None
        video_stream = None
        video_path = directory / "recording.mp4"
        try:
            # Only these newly created devices are shut down or deleted.
            udid = self.command("create", f"KitapDuy-CI-{uuid.uuid4().hex[:12]}",
                                device["deviceTypeIdentifier"], device["runtime"])
            if not re.fullmatch(r"[0-9a-fA-F-]{36}", udid):
                raise RuntimeError("simctl create returned an invalid device UUID.")
            result["udid"] = udid
            self.command("boot", udid)
            self.command("bootstatus", udid, "-b", timeout=300)
            self.command("install", udid, str(self.app))
            environment = dict(os.environ, SIMCTL_CHILD_KITAPDUY_CI_SMOKE_TEST="1",
                               SIMCTL_CHILD_KITAPDUY_CI_HTTPS_URL=self.url)
            console = directory / "console.log"
            console_stream = console.open("w", encoding="utf-8")
            process = subprocess.Popen(
                ["xcrun", "simctl", "launch", "--terminate-running-process", "--console", udid, self.bundle],
                stdout=console_stream, stderr=subprocess.STDOUT, env=environment,
            )
            video_process, video_stream = self.start_recording(udid, video_path)
            if not video_process:
                result["video_warning"] = "Could not start video recording."
            captured_screenshots = []
            def handle_marker(marker):
                filename = MILESTONE_SCREENSHOTS.get(marker)
                if filename:
                    screenshot_path = directory / filename
                    self.command("io", udid, "screenshot", str(screenshot_path), check=False)
                    captured_screenshots.append(filename)

            monitor_app(process, console, self.duration, on_marker=handle_marker)
            screenshot = directory / "screen.png"
            self.command("io", udid, "screenshot", str(screenshot))
            with screenshot.open("rb") as image:
                if image.read(8) != b"\x89PNG\r\n\x1a\n":
                    raise RuntimeError("The screenshot is not a PNG image.")
            # Recheck after capturing evidence, before deliberately terminating the app.
            monitor_app(process, console, 0)
            if video_process:
                if self.stop_recording(video_process, video_path):
                    result["video"] = "recording.mp4"
                else:
                    result["video_warning"] = "Video recording file was not generated or empty."
                video_process = None
            if video_stream:
                try:
                    video_stream.close()
                except Exception:
                    pass
                video_stream = None
            result["screenshots"] = captured_screenshots + ["screen.png"]
            result["passed"] = True
        except Exception as failure:
            result["error"] = f"{type(failure).__name__}: {failure}"
        finally:
            if video_process:
                try:
                    if self.stop_recording(video_process, video_path):
                        result["video"] = "recording.mp4"
                except Exception as failure:
                    result.setdefault("cleanup_warnings", []).append(str(failure))
                video_process = None
            if video_stream:
                try:
                    video_stream.close()
                except Exception:
                    pass
                video_stream = None
            if udid and re.fullmatch(r"[0-9a-fA-F-]{36}", udid):
                try:
                    if not result["passed"]:
                        self.command("io", udid, "screenshot", str(directory / "failure-screen.png"), check=False)
                    logs = self.command("spawn", udid, "log", "show", "--last", "3m", "--style", "compact",
                                        "--predicate", f'process == "{self.executable}"', timeout=30, check=False)
                    (directory / "system.log").write_text(logs, encoding="utf-8")
                    self.collect_crashes(udid, directory, started)
                except Exception as failure:
                    result["collection_warning"] = str(failure)
                # Cleanup failures must not prevent attempting the second device.
                for arguments in (("terminate", udid, self.bundle), ("shutdown", udid), ("delete", udid)):
                    try:
                        self.command(*arguments, timeout=30, check=False)
                    except Exception as failure:
                        result.setdefault("cleanup_warnings", []).append(str(failure))
            if process:
                try:
                    if process.poll() is None:
                        process.terminate()
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try:
                        process.kill()
                        process.wait(timeout=10)
                    except Exception as failure:
                        result.setdefault("cleanup_warnings", []).append(str(failure))
                except Exception as failure:
                    result.setdefault("cleanup_warnings", []).append(str(failure))
            if console_stream:
                console_stream.close()
            result["seconds"] = round(time.time() - started, 3)
        (directory / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
        print(f"{'PASS' if result['passed'] else 'FAIL'}: {device['name']} ({device['runtime']}) {result['error']}", flush=True)
        return result

    def run(self):
        snapshot = json.loads(self.command("list", "devices", "available", "--json"))
        (self.output / "devices.json").write_text(json.dumps(snapshot, indent=2), encoding="utf-8")
        # Do not stop on the first failure: retain iPhone and iPad evidence.
        return [self.run_device(device) for device in select_devices(snapshot["devices"])]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/ios-smoke"))
    parser.add_argument("--duration", type=int, default=60)
    parser.add_argument("--https-url", default=os.environ.get("IOS_SMOKE_HTTPS_URL", "https://example.com/"))
    args = parser.parse_args(argv)
    if args.duration < 45:
        parser.error("--duration must be at least 45 seconds to cover delayed startup crashes")
    output = args.output.resolve()
    try:
        url = urlsplit(args.https_url)
        if url.scheme != "https" or not url.hostname or url.username or url.password:
            raise ValueError("Use an HTTPS endpoint without embedded credentials.")
        results = SimulatorRunner(args.app.resolve(), output, args.duration, args.https_url).run()
    except Exception as failure:
        results = [{"name": "Simulator setup", "passed": False, "error": f"{type(failure).__name__}: {failure}"}]
        print(results[0]["error"], file=sys.stderr)
    write_reports(results, output)
    return 0 if len(results) == 2 and all(result["passed"] for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
