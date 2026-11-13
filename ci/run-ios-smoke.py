"""Launch the real simulator app on fresh iPhone/iPad devices and retain evidence."""

import argparse
import html
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

from ios_smoke_fixture import serve_fixture

UI_READY = "KITAPDUY_CI_UI_APPEARED"
NETWORK_PASS = "KITAPDUY_CI_NETWORK_PASS"
REQUIRED_MARKERS = (UI_READY, NETWORK_PASS)

FAILURE_MARKERS = (
    "KITAPDUY_CI_NETWORK_FAIL",
    "IrLinkageError",
    "Unhandled Kotlin exception caught by hook",
    "Uncaught Kotlin exception",
)

JOURNEYS = {
    "test01HomeDiscoverAndBookDetail": "Ana Sayfa, Keşfet ve kitap detayı",
    "test02BookPlayback": "Kitap oynatma, duraklatma ve atlama",
    "test03PodcastPlayback": "Podcast bölüm listesi ve oynatma",
    "test04Library": "Kitaplık sekmeleri ve raflar",
    "test05FeedAndProfile": "Akışta paylaşım ve profil kaydı",
    "test06ThemeAndPremium": "Koyu tema, Premium ve yasal metinler",
    "test07HomePodcasts": "Ana Sayfa podcast bölümü",
}

LEGACY_JOURNEYS = {
    "test01GuestAndBookPlayback": "Misafir, arama ve kitap oynatma",
    "test02PodcastAndLibrary": "Podcast ve Kitaplık",
    "test03FeedAndProfile": JOURNEYS["test05FeedAndProfile"],
    "test04ThemeAndPremium": JOURNEYS["test06ThemeAndPremium"],
}

SCREEN_GROUPS = (
    ("Giriş ve Ana Sayfa", "Önerilen kitaplara, duygu kartlarına ve podcast sırasına bak.", (
        ("01-guest-entry", "Misafir girişi"), ("02-guest-home", "Ana Sayfa · giriş sonrası"),
        ("22-home-catalog", "Önerilen kitaplar"), ("23-home-podcasts", "Podcast dünyası"),
    )),
    ("Keşfet ve kitap", "Arama alanını, boş sonucu, kapak yerleşimini ve oynatıcı kontrollerini karşılaştır.", (
        ("24-discover", "Keşfet"), ("03-search-empty-result", "Sonuçsuz arama"),
        ("04-book-search", "Kitap sonucu"), ("05-book-detail", "Kitap detayı"),
        ("25-book-player", "Kitap oynatıcı"), ("06-book-playing", "Kitap oynatma"),
        ("07-book-pause-and-skip", "Duraklatma ve 15 saniye atlama"), ("08-book-back-to-search", "Aramaya dönüş"),
    )),
    ("Podcast", "Bölüm listesini, oynatıcıyı, süreyi ve geri dönüşü incele.", (
        ("09-podcast-episodes", "Podcast bölümleri"), ("26-podcast-player", "Podcast oynatıcı"),
        ("10-podcast-playing", "Podcast oynatma"), ("31-podcast-back-to-search", "Podcastten aramaya dönüş"),
    )),
    ("Kitaplık", "KitapDuy3’teki Favoriler, İndirilenler, Geçmiş ve raf akışlarıyla karşılaştır.", (
        ("27-library-favorites", "Favoriler"), ("11-library-downloads-empty", "İndirilenler · boş durum"),
        ("28-library-history", "Geçmiş"), ("12-library-shelves", "Bitirdiklerim rafı"),
    )),
    ("Akış ve Profil", "Gönderi yazma, yayınlama, profil ve düzenleme akışlarını kontrol et.", (
        ("13-feed", "Edebiyat Duvarı"), ("14-feed-compose", "Gönderi düzenleyici"),
        ("15-feed-published", "Yayınlanan gönderi"), ("29-profile", "Okur köşem"),
        ("16-profile-edit", "Profili düzenle"), ("17-profile-saved", "Kaydedilen profil"),
    )),
    ("Ayarlar ve Premium", "Açık ve koyu temayı, planları, geri yüklemeyi ve yasal metinleri incele.", (
        ("18-settings-light", "Ayarlar · açık tema"), ("19-settings-dark", "Ayarlar · koyu tema"),
        ("20-premium", "Premium planları"), ("30-premium-terms", "Kullanım koşulları"),
        ("21-premium-legal", "Gizlilik politikası"),
    )),
)


def journey_outcomes(log):
    outcomes = {}
    for test, outcome in re.findall(r"^Test Case '-\[\S+ (\w+)\]' (passed|failed|skipped)\b", log, re.MULTILINE):
        # Keep a failure visible if a test was repeated.
        if outcomes.get(test) != "failed":
            outcomes[test] = outcome
    return outcomes


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


def monitor_app(process, console, duration, clock=time.monotonic, sleep=time.sleep):
    deadline = clock() + duration
    while True:
        output = console.read_text(encoding="utf-8", errors="replace")
        for marker in FAILURE_MARKERS:
            if marker in output:
                raise RuntimeError(f"App log contains a failure marker: {marker}")
        if process.poll() is not None:
            raise RuntimeError("App exited during the startup and HTTPS check.")
        missing = [marker for marker in REQUIRED_MARKERS if marker not in output]
        if not missing:
            return
        if clock() >= deadline:
            raise RuntimeError("Missing app evidence: " + ", ".join(missing))
        sleep(1)


def read_test_summary(bundle):
    completed = subprocess.run(["xcrun", "xcresulttool", "get", "test-results", "summary", "--path", str(bundle)],
                               check=True, timeout=45, capture_output=True, text=True, encoding="utf-8")
    summary = json.loads(completed.stdout)
    return {key: summary[key] for key in ("totalTestCount", "passedTests", "failedTests", "skippedTests")}


def export_evidence(bundle, directory):
    exported = directory / "xcode-attachments"
    subprocess.run(["xcrun", "xcresulttool", "export", "attachments", "--path", str(bundle),
                    "--output-path", str(exported)], check=True, timeout=90, capture_output=True, text=True)
    manifest = json.loads((exported / "manifest.json").read_text(encoding="utf-8"))
    screenshots = []
    audits = []
    for test in manifest:
        for attachment in test.get("attachments", []):
            source = exported / Path(attachment["exportedFileName"]).name
            if not source.is_file():
                continue
            label = attachment.get("suggestedHumanReadableName") or source.stem
            stem = re.sub(r"_\d+_[0-9a-fA-F-]{36}$", "", Path(label).stem)
            stem = re.sub(r"[^a-zA-Z0-9._-]", "-", stem).strip("-.") or "attachment"
            if stem.startswith("audit-"):
                audits.append(f"{stem}\n{source.read_text(encoding='utf-8', errors='replace')}")
                continue
            if source.suffix.lower() != ".png" or not re.match(r"^(failure-)?\d{2}-", stem):
                continue
            filename = f"{stem}.png"
            if (directory / filename).exists():
                filename = f"{stem}-{len(screenshots) + 1}.png"
            shutil.copy2(source, directory / filename)
            screenshots.append(filename)
    if audits:
        (directory / "accessibility.txt").write_text("\n\n".join(audits), encoding="utf-8")
    return screenshots


def write_reports(results, output):
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    write_gallery(results, output)
    suite = ET.Element("testsuite", name="iOS user journeys")
    for result in results:
        outcomes = result.get("journeys", {})
        if outcomes:
            append_journey_cases(suite, result, outcomes)
        if not outcomes or (not result["passed"] and "failed" not in outcomes.values()):
            case = ET.SubElement(suite, "testcase", classname="KitapDuy.SimulatorSmoke",
                                 name=result["name"], time=str(result.get("seconds", 0)))
            if not result["passed"]:
                ET.SubElement(case, "failure", message=result["error"]).text = result["error"]
            ET.SubElement(case, "system-out").text = json.dumps(result, indent=2)
    suite.set("tests", str(len(suite)))
    suite.set("failures", str(len(suite.findall("testcase/failure"))))
    suite.set("skipped", str(len(suite.findall("testcase/skipped"))))
    ET.ElementTree(suite).write(output / "junit.xml", encoding="utf-8", xml_declaration=True)


def append_journey_cases(suite, result, outcomes):
    titles = journey_titles(outcomes)
    for test, label in titles.items():
        case = ET.SubElement(suite, "testcase", classname=f'KitapDuy.{result["family"]}', name=label)
        outcome = outcomes.get(test)
        if outcome == "failed":
            detail = next((line for line in result["error"].splitlines() if f" {test}]" in line),
                          "See the XCUITest log for the failing step.")
            ET.SubElement(case, "failure", message=detail).text = detail
        elif outcome != "passed":
            ET.SubElement(case, "skipped", message="No completed result")


def journey_titles(outcomes):
    return LEGACY_JOURNEYS if outcomes.keys() & LEGACY_JOURNEYS.keys() else JOURNEYS


def write_gallery(results, output):
    sections = []
    devices = {result["family"]: set(result.get("screenshots", [])) for result in results if result.get("family")}
    reference = output / "Android"
    has_android = any((reference / f"{step}.png").is_file()
                      for _, _, screens in SCREEN_GROUPS for step, _ in screens)
    outcome_labels = {"passed": "Geçti", "failed": "Hata", "skipped": "Atlandı"}
    for result in results:
        family = result.get("family")
        status = "Geçti" if result["passed"] else "Başarısız"
        video = ""
        if result.get("video"):
            video = (f'<video controls preload="none" src="{family}/recording.mp4"></video>'
                     f'<p><a href="{family}/recording.mp4">Video dosyası</a></p>')
        links = []
        if family and "failure-screen.png" in devices[family]:
            links.append(f'<a href="{family}/failure-screen.png">Hata sonundaki ekran</a>')
        for filename, label in (("uitest.log", "XCUITest günlüğü"), ("console.log", "Başlangıç günlüğü"),
                                ("accessibility.txt", "Erişilebilirlik bulguları")):
            if family and (output / family / filename).exists():
                links.append(f'<a href="{family}/{filename}">{label}</a>')
        tests = result.get("xcuitest")
        progress = f'<p>{tests["passedTests"]} / {tests["totalTestCount"]} senaryo geçti.</p>' if tests else ""
        outcomes = result.get("journeys", {})
        journey_list = "".join(f'<li>{label} <strong>{outcome_labels.get(outcomes.get(test), "Sonuç yok")}</strong></li>'
                               for test, label in journey_titles(outcomes).items()) if family else ""
        warnings = "\n".join(result[key] for key in ("error", "video_warning", "screenshot_warning",
                                                     "collection_warning") if result.get(key))
        details = f'<details><summary>Hata ayrıntıları</summary><pre>{html.escape(warnings)}</pre></details>' if warnings else ""
        device_id = f' id="{family}"' if family else ""
        sections.append(f'<section{device_id}><h2>{html.escape(result["name"])} · {status}</h2>'
                        f'<p>{html.escape(result.get("runtime", ""))}</p>{progress}<ul>{journey_list}</ul>'
                        f'{details}{video}<p>{" · ".join(links)}</p></section>')

    # Align the same step across devices and make gaps visible even on a failed run.
    groups = []
    android_header = '<th scope="col">KitapDuy3 · Android</th>' if has_android else ""
    for index, (title, guide, screens) in enumerate(SCREEN_GROUPS, start=1):
        rows = []
        for step, label in screens:
            cells = []
            for family in ("iPhone", "iPad"):
                available = devices.get(family, set())
                pictures = []
                for name, state in ((f"{step}.png", "Ekran"), (f"failure-{step}.png", "Hata anı")):
                    if name in available:
                        pictures.append(f'<a href="{family}/{name}"><img loading="lazy" src="{family}/{name}" '
                                        f'alt="{family} · {label} · {state}">{state}</a>')
                cells.append(f'<td>{"".join(pictures) or "Görüntü alınamadı"}</td>')
            if has_android:
                name = f"{step}.png"
                picture = (f'<a href="Android/{name}"><img loading="lazy" src="Android/{name}" '
                           f'alt="KitapDuy3 · {label}">Referans</a>') if (reference / name).is_file() else "Referans yok"
                cells.append(f'<td>{picture}</td>')
            rows.append(f'<tr><th scope="row">{label}</th>{"".join(cells)}</tr>')
        groups.append(f'<section id="screens-{index}"><h2>{title}</h2><p>{guide}</p><table>'
                      f'<thead><tr><th scope="col">Ekran</th><th scope="col">iPhone</th><th scope="col">iPad</th>{android_header}</tr></thead>'
                      f'<tbody>{"".join(rows)}</tbody></table></section>')

    navigation = " · ".join([f'<a href="#{family}">{family} sonuçları</a>' for family in devices] +
                             [f'<a href="#screens-{index}">{title}</a>'
                              for index, (title, _, _) in enumerate(SCREEN_GROUPS, start=1)])
    page = """<!doctype html><html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>KitapDuy iOS kullanıcı deneyimi</title><style>
body{font:16px system-ui;max-width:1080px;margin:32px auto;padding:0 16px;color:#29251f;background:#faf7f1}
a{color:#704527}nav{line-height:2}section{padding:16px 0}li{padding-block:3px}li strong{margin-inline-start:8px}
video{width:min(100%,680px);max-height:460px;background:#221d18}pre{white-space:pre-wrap;overflow-wrap:anywhere}
table{width:100%;table-layout:fixed;border-collapse:collapse;background:#fff}th,td{padding:8px;border:1px solid #dfd6cb;text-align:left}
th:first-child{width:24%}td a{display:block;margin:6px 0}img{display:block;width:100%;height:clamp(110px,36vw,320px);object-fit:contain}
</style><h1>KitapDuy iOS kullanıcı deneyimi</h1>
<p>Gerçek iPhone ve iPad simülatörü görüntülerini KitapDuy3 ile karşılaştırmak için ekranlara tıkla.</p>
<p>Senaryolar yerel örnek veriyle çalışır. Canlı hesap girişi, mağaza işlemleri ve fiziksel cihaz davranışı ayrı doğrulama gerektirir.</p>"""
    reference_info = reference / "source.txt"
    provenance = ""
    if has_android and reference_info.is_file():
        provenance = f'<p>{html.escape(reference_info.read_text(encoding="utf-8", errors="replace").strip())}</p>'
    findings_path = output / "observations.txt"
    findings = ""
    if findings_path.is_file():
        observations = findings_path.read_text(encoding="utf-8").splitlines()
        findings = '<section><h2>Karşılaştırma bulguları</h2><ul>' + "".join(
            f'<li>{html.escape(line)}</li>' for line in observations if line.strip()
        ) + '</ul></section>'
    (output / "index.html").write_text(page + provenance + f'<nav aria-label="Rapor bölümleri">{navigation}</nav>' +
                                       findings + "".join(sections + groups) + "</html>", encoding="utf-8")


class SimulatorRunner:
    def __init__(self, app, output, duration, url, test_run=None):
        self.app, self.output, self.duration, self.url = app, output, duration, url
        self.test_run = test_run
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
        self.last_video_warning = ""
        log_stream = None
        try:
            if video_path.exists():
                try:
                    video_path.unlink()
                except Exception:
                    pass
            log_path = video_path.with_name("recordVideo.log")
            if log_path.exists():
                try:
                    log_path.unlink()
                except Exception:
                    pass
            log_stream = log_path.open("w", encoding="utf-8")
            process = subprocess.Popen(
                ["xcrun", "simctl", "io", udid, "recordVideo", "--codec=h264", "--force", str(video_path)],
                stdout=log_stream,
                stderr=subprocess.STDOUT,
            )
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Started video recording on {udid} (pid={getattr(process, 'pid', 'unknown')}) -> {video_path}\n")
            return process, log_stream
        except Exception as error:
            if log_stream:
                try:
                    log_stream.close()
                except Exception:
                    pass
            self.last_video_warning = f"Failed to start video recording: {error}"
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Failed to start video recording on {udid}: {error}\n")
            return None, None

    def stop_recording(self, process, video_path, log_stream=None):
        """Cleanly stop the video recording subprocess so the MP4 trailer is written."""
        self.last_video_warning = ""
        if not process:
            self.last_video_warning = "No video recording process to stop."
            return False
        try:
            premature_exit = process.poll() is not None
            if not premature_exit:
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
            else:
                rc = getattr(process, "returncode", None)
                if rc is not None and rc != 0:
                    with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                        log.write(f"Video recording process exited prematurely with code {rc}\n")
            if log_stream:
                try:
                    log_stream.close()
                except Exception:
                    pass
            log_path = video_path.with_name("recordVideo.log")
            log_output = ""
            if log_path.exists():
                try:
                    log_output = log_path.read_text(encoding="utf-8", errors="replace").strip()
                except Exception:
                    pass
            if not video_path.exists() or video_path.stat().st_size == 0:
                self.last_video_warning = f"Video recording file was not generated or empty. {log_output}".strip()
                return False
            rc = getattr(process, "returncode", None)
            if premature_exit and isinstance(rc, int) and rc != 0:
                self.last_video_warning = f"Video recording exited prematurely with code {rc}. {log_output}".strip()
                return False
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Video recording completed successfully: {video_path} ({video_path.stat().st_size} bytes)\n")
            return True
        except Exception as error:
            self.last_video_warning = f"Error stopping video recording: {error}"
            with (self.output / "runner.log").open("a", encoding="utf-8") as log:
                log.write(f"Error stopping video recording: {error}\n")
            return False

    def run_device(self, device):
        directory = self.output / device["family"]
        directory.mkdir(parents=True, exist_ok=True)
        result = {"name": device["name"], "family": device["family"], "runtime": device["runtime"],
                  "passed": False, "error": "", "artifacts": str(directory), "screenshots": []}
        if os.environ.get("CM_COMMIT"):
            result["commit"] = os.environ["CM_COMMIT"]
        started = time.time()
        udid = None
        launch = None
        video_process = None
        video_stream = None
        video_path = directory / "recording.mp4"
        bundle = directory / "journey.xcresult"
        try:
            candidate = self.command("create", f"KitapDuy-CI-{uuid.uuid4().hex[:12]}",
                                     device["deviceTypeIdentifier"], device["runtime"])
            # Xcode matches simulator destinations against their uppercase CoreSimulator UUIDs.
            udid = str(uuid.UUID(candidate)).upper()
            result["udid"] = udid
            self.command("boot", udid)
            self.command("bootstatus", udid, timeout=300)
            self.command("status_bar", udid, "override", "--time", "9:41", "--batteryState", "unplugged",
                         "--batteryLevel", "100", check=False)
            self.command("install", udid, str(self.app))
            with serve_fixture() as (fixture_url, requests):
                console = directory / "console.log"
                with console.open("w", encoding="utf-8") as stream:
                    environment = dict(os.environ, SIMCTL_CHILD_KITAPDUY_CI_SMOKE_TEST="1",
                                       SIMCTL_CHILD_KITAPDUY_CI_FIXTURE_URL=fixture_url,
                                       SIMCTL_CHILD_KITAPDUY_CI_HTTPS_URL=self.url)
                    launch = subprocess.Popen(
                        ["xcrun", "simctl", "launch", "--terminate-running-process", "--console-pty", udid, self.bundle],
                        stdout=stream, stderr=subprocess.STDOUT, env=environment,
                    )
                    monitor_app(launch, console, self.duration)
                self.command("terminate", udid, self.bundle, check=False)
                try:
                    launch.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    launch.terminate()
                    launch.wait(timeout=5)
                launch = None

                video_process, video_stream = self.start_recording(udid, video_path)
                if not video_process:
                    result["video_warning"] = self.last_video_warning
                environment = dict(os.environ, TEST_RUNNER_KITAPDUY_CI_FIXTURE_URL=fixture_url)
                test_log = directory / "uitest.log"
                with test_log.open("w", encoding="utf-8") as log:
                    completed = subprocess.run(
                        ["xcodebuild", "test-without-building", "-xctestrun", str(self.test_run),
                         "-destination", f"platform=iOS Simulator,id={udid}",
                         "-parallel-testing-enabled", "NO", "-resultBundlePath", str(bundle)],
                        env=environment, stdout=log, stderr=subprocess.STDOUT, timeout=900,
                    )
                result["fixture_requests"] = list(requests)
                log_text = test_log.read_text(encoding="utf-8", errors="replace")
                result["journeys"] = journey_outcomes(log_text)
                if completed.returncode:
                    errors = [line.strip()[:500] for line in log_text.splitlines() if ": error: " in line]
                    detail = "\n" + "\n".join(dict.fromkeys(errors[-8:])) if errors else ""
                    raise RuntimeError(f"XCUITest failed ({completed.returncode}). See uitest.log and screenshots.{detail}")
                result["xcuitest"] = read_test_summary(bundle)
                tests = result["xcuitest"]
                if (tests["passedTests"] != tests["totalTestCount"] or
                        any(result["journeys"].get(test) != "passed" for test in JOURNEYS)):
                    raise RuntimeError("Xcode did not run and pass the complete journey suite.")
                missing = set(("guest_signup", "create_post", "update_profile")) - set(requests)
                if missing:
                    raise RuntimeError("Missing requests to local test server: " + ", ".join(sorted(missing)))
                result["passed"] = True
        except Exception as failure:
            result["error"] = f"{type(failure).__name__}: {failure}"
        finally:
            if udid and not result["passed"]:
                try:
                    screenshot = directory / "failure-screen.png"
                    self.command("io", udid, "screenshot", str(screenshot))
                    if screenshot.is_file():
                        result["screenshots"].append(screenshot.name)
                except Exception as failure:
                    result["screenshot_warning"] = str(failure)
            if video_process:
                try:
                    if self.stop_recording(video_process, video_path, video_stream):
                        result["video"] = video_path.name
                    else:
                        result["video_warning"] = self.last_video_warning
                finally:
                    if video_stream:
                        video_stream.close()
            if bundle.is_dir():
                if "xcuitest" not in result:
                    try:
                        result["xcuitest"] = read_test_summary(bundle)
                    except Exception as failure:
                        result.setdefault("collection_warning", str(failure))
                try:
                    result["screenshots"].extend(export_evidence(bundle, directory))
                except Exception as failure:
                    result["screenshot_warning"] = str(failure)
                try:
                    shutil.make_archive(str(directory / "journey.xcresult"), "zip", directory, bundle.name)
                    shutil.rmtree(bundle)
                except OSError as failure:
                    result.setdefault("cleanup_warnings", []).append(str(failure))
            if udid:
                try:
                    logs = self.command("spawn", udid, "log", "show", "--last", "15m", "--style", "compact",
                                        "--predicate", f'process == "{self.executable}"', timeout=30, check=False)
                    (directory / "system.log").write_text(logs, encoding="utf-8")
                    self.collect_crashes(udid, directory, started)
                except Exception as failure:
                    result["collection_warning"] = str(failure)
                for arguments in (("terminate", udid, self.bundle), ("shutdown", udid), ("delete", udid)):
                    try:
                        self.command(*arguments, timeout=30, check=False)
                    except Exception as failure:
                        result.setdefault("cleanup_warnings", []).append(str(failure))
            if launch and launch.poll() is None:
                try:
                    launch.kill()
                    launch.wait(timeout=10)
                except (OSError, subprocess.TimeoutExpired) as failure:
                    result.setdefault("cleanup_warnings", []).append(str(failure))
            result["seconds"] = round(time.time() - started, 3)
        if result["passed"] and (not result["screenshots"] or not result.get("video")):
            result["passed"] = False
            result["error"] = "Test actions passed, but video or milestone screenshots are missing."
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
    parser.add_argument("--test-run", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/ios-smoke"))
    parser.add_argument("--android-reference", type=Path, default=Path("ci/kitapduy3-reference"),
                        help="Directory of Android reference PNGs named after report steps.")
    parser.add_argument("--duration", type=int, default=125)
    parser.add_argument("--https-url", default=os.environ.get("IOS_SMOKE_HTTPS_URL", "https://example.com/"))
    args = parser.parse_args(argv)
    if args.duration < 1:
        parser.error("--duration must be positive")
    output = args.output.resolve()
    try:
        url = urlsplit(args.https_url)
        if url.scheme != "https" or not url.hostname or url.username or url.password:
            raise ValueError("Use an HTTPS endpoint without embedded credentials.")
        if not args.test_run.is_file():
            raise ValueError("The Xcode test configuration (.xctestrun) is missing.")
        results = SimulatorRunner(args.app.resolve(), output, args.duration, args.https_url, args.test_run.resolve()).run()
    except Exception as failure:
        results = [{"name": "Simulator setup", "passed": False, "error": f"{type(failure).__name__}: {failure}"}]
        print(results[0]["error"], file=sys.stderr)
    if args.android_reference.is_dir():
        reference = output / "Android"
        try:
            reference.mkdir(parents=True, exist_ok=True)
            filenames = ["source.txt", *(f"{step}.png" for _, _, screens in SCREEN_GROUPS for step, _ in screens)]
            for filename in filenames:
                source = args.android_reference / filename
                destination = reference / filename
                if source.is_file() and source.resolve() != destination.resolve():
                    shutil.copy2(source, destination)
        except OSError as failure:
            print(f"Android reference unavailable: {failure}", file=sys.stderr)
    write_reports(results, output)
    return 0 if len(results) == 2 and all(result["passed"] for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
