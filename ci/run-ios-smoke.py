"""Launch the real simulator app on fresh iPhone/iPad devices and retain evidence."""

import argparse
from collections import Counter
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
    "test08AdvisorArenaAndAskida": "Edebi Danışman, KitapDuy Arena ve Askıda Kitap",
    "test09LiteraryAdvisorDeep": "Edebi Danışman · kapsamlı inceleme",
    "test10BookDetailFeaturesAndMiniPlayer": "Kitap detayı kontrolleri ve Mini Player",
    "test11SearchUsersAndPopulatedLibrary": "Kitap iste, okur profilleri ve dolu kitaplık",
    "test12FeedDeepNotificationsAndPostDetail": "Akış anketi, bildirimler ve gönderi yorumları",
    "test13ProfileWrappedArenaAndAskidaDeep": "Yıllık Özet, Arena ve Askıda Kitap derin tarama",
    "test14DarkModeFullAudit": "Koyu tema · tüm ana ekranlar denetimi",
}

LEGACY_JOURNEYS = {
    "test01GuestAndBookPlayback": "Misafir, arama ve kitap oynatma",
    "test02PodcastAndLibrary": "Podcast ve Kitaplık",
    "test03FeedAndProfile": JOURNEYS["test05FeedAndProfile"],
    "test04ThemeAndPremium": JOURNEYS["test06ThemeAndPremium"],
}

SCREEN_GROUPS = (
    ("Giriş ve Ana Sayfa", "Giriş ekranını, önerilen kitapları, kategori/yazar/ruh hali detaylarını ve podcast sırasını incele.", (
        ("01-guest-entry", "Misafir girişi"), ("38-welcome-login", "Karşılama ve E-posta girişi doğrulama"),
        ("02-guest-home", "Ana Sayfa · giriş sonrası"), ("22-home-catalog", "Önerilen kitaplar"),
        ("23-home-podcasts", "Podcast dünyası"), ("42-category-detail", "Kategori detayı (Polisiye)"),
        ("43-author-detail", "Yazar detayı"), ("44-mood-detail", "Ruh hali detayı (Huzurlu)"),
        ("45-home-articles-check", "Ana Sayfa makale kontrolü"),
    )),
    ("Edebi Danışman · Derin İnceleme", "Tüm giriş noktalarını, modları, derinlik/spoiler ayarlarını, diyalogları ve kitap önerisi geçişlerini incele.", (
        ("46-advisor-welcome", "Edebi Danışman · karşılama ve modlar"),
        ("47-advisor-info-dialog", "Edebi Danışman · Hakkında diyaloğu"),
        ("48-advisor-analysis-and-reading", "Eser Analizi ve Okuma Görünümü"),
        ("49-advisor-brainstorm-debate", "Beyin Fırtınası ve Okuma Rotası"),
        ("50-advisor-custom-query-and-book", "Özel soru, derinlik ve kitap önerisi"),
    )),
    ("Keşfet ve kitap", "Arama alanını, Kitap Talep Et akışını, okur profillerini, kitap detayını, oynatıcıyı ve Mini Player'ı incele.", (
        ("24-discover", "Keşfet"), ("24b-discover-categories", "Keşfet · kategoriler"),
        ("03-search-empty-result", "Sonuçsuz arama"),
        ("55-search-book-request", "Keşfet · Kitap Talep Et"),
        ("56-search-users", "Keşfet · Okurlar araması"),
        ("57-other-user-profile", "Okur profili · Takip et/bırak"),
        ("04-book-search", "Kitap sonucu"), ("05-book-detail", "Kitap detayı"),
        ("51-book-summary-and-like", "1 Dakikada Eser Özeti ve beğeni"),
        ("52-book-dialogs-and-shelf", "Hız, uyku, alıntı ve rafa ekle"),
        ("53-book-comment-posted", "Kitap yorumu gönderimi"),
        ("25-book-player", "Kitap oynatıcı"), ("06-book-playing", "Kitap oynatma"),
        ("07-book-pause-and-skip", "Duraklatma ve 15 saniye atlama"),
        ("54-mini-player", "Mini Player denetimi"),
        ("08-book-back-to-search", "Aramaya dönüş"),
    )),
    ("Danışman, Arena ve Askıda", "Ana Sayfa ve Keşfet’ten Edebi Danışman’a, KitapDuy Arena modlarına ve Askıda Kitap akışlarına bak.", (
        ("32-home-advisor", "Edebi Danışman · Ana Sayfa"),
        ("33-discover-advisor", "Edebi Danışman · Keşfet"),
        ("34-arena-menu", "KitapDuy Arena · Menü"),
        ("35-arena-playing", "KitapDuy Arena · Soru"),
        ("63-arena-leaderboard-and-modes", "Arena · Liderlik, Turnuva ve Blitz"),
        ("36-askida-list", "Askıda Kitap · Liste"),
        ("37-askida-donate", "Askıda Kitap · Bağış"),
        ("64-askida-search-claim-and-donate", "Askıda · arama, talep, sohbet ve bağış"),
    )),
    ("Podcast", "Bölüm listesini, oynatıcı kontrollerini, süreyi ve geri dönüşü incele.", (
        ("09-podcast-episodes", "Podcast bölümleri"), ("26-podcast-player", "Podcast oynatıcı"),
        ("10-podcast-playing", "Podcast oynatma"), ("39-podcast-controls", "Podcast kontrolleri"),
        ("31-podcast-back-to-search", "Podcastten aramaya dönüş"),
    )),
    ("Kitaplık", "KitapDuy3’teki Favoriler, İndirilenler, Geçmiş ve raf akışlarıyla karşılaştır.", (
        ("27-library-favorites", "Favoriler"), ("11-library-downloads-empty", "İndirilenler · boş durum"),
        ("28-library-history", "Geçmiş"), ("12-library-shelves", "Bitirdiklerim rafı"),
        ("58-library-populated", "Dolu kitaplık ve övünç rafı"),
    )),
    ("Akış ve Profil", "Gönderi/anket yazma, yorum CRUD, bildirimler, Yıllık Özet ve profil düzenleme akışlarını kontrol et.", (
        ("13-feed", "Edebiyat Duvarı"), ("14-feed-compose", "Gönderi düzenleyici"),
        ("15-feed-published", "Yayınlanan gönderi"),
        ("59-feed-poll-published", "Akış · anket paylaşımı ve oylama"),
        ("60-post-detail-comments", "Gönderi detayı ve yorum CRUD"),
        ("61-notifications", "Bildirimler · tümünü okundu işaretle"),
        ("29-profile", "Okur köşem"), ("62-profile-wrapped", "Okur köşem · Yıllık Özet ve paylaşımlar"),
        ("16-profile-edit", "Profili düzenle"), ("17-profile-saved", "Kaydedilen profil"),
    )),
    ("Ayarlar ve Premium", "Açık/koyu tema, istatistikler, hesap silme diyaloğu, planlar ve yasal metinleri incele.", (
        ("18-settings-light", "Ayarlar · açık tema"), ("19-settings-dark", "Ayarlar · koyu tema"),
        ("40-settings-statistics", "Ayarlar · İstatistikler"),
        ("41-settings-delete-dialog", "Ayarlar · Hesabı Sil diyaloğu"),
        ("20-premium", "Premium planları"), ("30-premium-terms", "Kullanım koşulları"),
        ("21-premium-legal", "Gizlilik politikası"),
    )),
    ("Koyu Tema · Tam Denetim", "Ana ekranların, Edebi Danışmanın, kitap detayının ve akışın koyu tema kontrast ve yerleşimini incele.", (
        ("65-dark-home", "Koyu tema · Ana Sayfa"),
        ("66-dark-advisor", "Koyu tema · Edebi Danışman"),
        ("66-dark-advisor-reply", "Koyu tema · Danışman yanıtı"),
        ("67-dark-discover", "Koyu tema · Keşfet"),
        ("68-dark-book-detail", "Koyu tema · Kitap detayı"),
        ("68-dark-feed", "Koyu tema · Edebiyat Duvarı"),
    )),
)

TRACKED_FIXTURE_EVENTS = (
    ("guest_signup", "Misafir oturumu açılışı (`POST /auth/v1/signup`)"),
    ("update_profile", "Profil bilgisi güncellemesi (`PATCH/POST /rest/v1/users`)"),
    ("create_post", "Akışta gönderi/anket oluşturma (`POST /rest/v1/posts`)"),
    ("update_post", "Gönderi güncellemesi, beğeni veya anket oyu (`PATCH /rest/v1/posts`)"),
    ("delete_post", "Gönderi silme (`DELETE /rest/v1/posts`)"),
    ("create_comment", "Yorum ekleme (`POST /rest/v1/feed_comments`)"),
    ("delete_comment", "Yorum silme (`DELETE /rest/v1/feed_comments`)"),
    ("add_shelf", "Rafa kitap ekleme (`POST /rest/v1/user_bookshelves`)"),
    ("follow_user", "Okur takip etme (`POST /rest/v1/followers`)"),
    ("unfollow_user", "Okur takibi bırakma (`DELETE /rest/v1/followers`)"),
    ("mark_notifications_read", "Bildirimleri okundu işaretleme (`PATCH /rest/v1/notifications`)"),
    ("donate_askida", "Askıda Kitap bağışı (`POST /rest/v1/askida_kitap`)"),
    ("send_askida_chat", "Askıda Kitap sohbet mesajı (`POST /rest/v1/askida_mesaj`)"),
    ("create_quiz_match", "Arena düello odası oluşturma (`POST /rest/v1/quiz_matches`)"),
)

UNREACHABLE_CODEBASE_SCREENS = (
    "EntertainmentTab",
    "AskidaTab",
    "KitapBilScreen",
    "MindLibraryScreen",
    "ScienceArticlesScreen",
    "GuessAuthorScreen",
    "IndependentPensScreen",
    "PodcastListScreen",
    "FocusScreen",
    "ClubsScreen",
    "ClubDetailScreen",
    "ChatScreen",
    "DownloadsScreen",
    "OnboardingScreen",
)


def journey_outcomes(log):
    outcomes = {}
    for test, outcome in re.findall(r"^Test Case '-\[\S+ (\w+)\]' (passed|failed|skipped)\b", log, re.MULTILINE):
        # Keep a failure visible if a test was repeated.
        if outcomes.get(test) != "failed":
            outcomes[test] = outcome
    return outcomes


DEVICE_SPECS = (
    ("iPhone", "iPhone 17 Pro Max"),
    ("iPad", "iPad Air 11-inch (M3)"),
)

DEVICE_PRESETS = {
    "both": ("iPhone", "iPad"),
    "iphone": ("iPhone",),
    "ipad": ("iPad",),
}


def parse_device_families(value):
    key = "both" if value is None else str(value).strip().lower()
    if not key:
        key = "both"
    if key not in DEVICE_PRESETS:
        allowed = ", ".join(DEVICE_PRESETS)
        raise ValueError(f"Unsupported --devices value {value!r}; expected one of: {allowed}")
    return DEVICE_PRESETS[key]


def select_devices(devices, families=("iPhone", "iPad")):
    """Use the newest installed iOS runtime that has every requested device family."""
    target_families = tuple(dict.fromkeys(families))
    if not target_families:
        raise ValueError("At least one device family is required.")
    preferred_by_family = dict(DEVICE_SPECS)
    unknown = [family for family in target_families if family not in preferred_by_family]
    if unknown:
        raise ValueError(f"Unsupported device families: {', '.join(unknown)}")
    runtimes = sorted(
        (key for key in devices if re.search(r"\.iOS-\d", key)),
        key=lambda key: tuple(int(part) for part in re.findall(r"\d+", key)),
        reverse=True,
    )
    for runtime in runtimes:
        available = [device for device in devices[runtime]
                     if device.get("isAvailable") and device.get("deviceTypeIdentifier")]
        selected = []
        for family in target_families:
            preferred = preferred_by_family[family]
            matches = [device for device in available if device["name"].startswith(family)]
            if not matches:
                break
            device = next((device for device in matches if device["name"] == preferred), matches[0])
            selected.append(dict(device, runtime=runtime, family=family))
        if len(selected) == len(target_families):
            return selected
    if target_families == ("iPhone", "iPad"):
        raise RuntimeError("No installed iOS runtime has both an available iPhone and iPad. Inspect devices.json.")
    needed = " and ".join(target_families)
    raise RuntimeError(f"No installed iOS runtime has an available {needed}. Inspect devices.json.")


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


def _normalize_probe_record(item, fallback_journey="unknown"):
    if not isinstance(item, dict):
        return {
            "journey": fallback_journey,
            "screen": "",
            "theme": "?",
            "category": "Probe",
            "status": "INFO",
            "summary": str(item),
            "detail": "",
        }
    status = str(item.get("status", "INFO")).upper()
    if status not in ("FAIL", "WARN", "INFO", "PASS"):
        status = "INFO"
    return {
        "journey": str(item.get("journey") or fallback_journey),
        "screen": str(item.get("screen") or ""),
        "theme": str(item.get("theme") or "?"),
        "category": str(item.get("category") or "Genel"),
        "status": status,
        "summary": str(item.get("summary") or ""),
        "detail": str(item.get("detail") or ""),
    }


def _format_probes_text(probes):
    lines = []
    for item in probes:
        line = (f"[{item['status']}] [{item['theme']}] {item['screen']} "
                f"({item['journey']} · {item['category']}) — {item['summary']}")
        lines.append(line)
        if item.get("detail"):
            lines.append(f"    Detay: {item['detail']}")
    return "\n".join(lines)


def export_evidence(bundle, directory):
    exported = directory / "xcode-attachments"
    subprocess.run(["xcrun", "xcresulttool", "export", "attachments", "--path", str(bundle),
                    "--output-path", str(exported)], check=True, timeout=90, capture_output=True, text=True)
    manifest = json.loads((exported / "manifest.json").read_text(encoding="utf-8"))
    screenshots = []
    audits = []
    probes = []
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
            if stem.startswith("probe-"):
                raw = source.read_text(encoding="utf-8", errors="replace")
                fallback_journey = stem[len("probe-"):] or stem
                try:
                    payload = json.loads(raw)
                except json.JSONDecodeError:
                    probes.append({
                        "journey": fallback_journey,
                        "screen": stem,
                        "theme": "?",
                        "category": "ProbeParse",
                        "status": "WARN",
                        "summary": f"{stem} JSON ayrıştırılamadı",
                        "detail": raw[:400],
                    })
                else:
                    items = payload if isinstance(payload, list) else [payload]
                    probes.extend(_normalize_probe_record(item, fallback_journey) for item in items)
                continue
            if source.suffix.lower() != ".png" or not re.match(r"^(failure-)?\d{2}[a-z0-9]*-", stem):
                continue
            filename = f"{stem}.png"
            if (directory / filename).exists():
                filename = f"{stem}-{len(screenshots) + 1}.png"
            shutil.copy2(source, directory / filename)
            screenshots.append(filename)
    if audits:
        (directory / "accessibility.txt").write_text("\n\n".join(audits), encoding="utf-8")
    if probes:
        (directory / "probes.json").write_text(json.dumps(probes, ensure_ascii=False, indent=2), encoding="utf-8")
        (directory / "probes.txt").write_text(_format_probes_text(probes), encoding="utf-8")
    return screenshots


def _load_device_probes(result, output):
    if isinstance(result.get("probes"), list):
        return [_normalize_probe_record(item) for item in result["probes"]]
    family = result.get("family")
    if not family:
        return []
    probes_path = output / family / "probes.json"
    if not probes_path.is_file():
        return []
    try:
        payload = json.loads(probes_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return []
    items = payload if isinstance(payload, list) else [payload]
    return [_normalize_probe_record(item) for item in items]


def _load_accessibility_sections(result, output):
    text = result.get("accessibility_text")
    if text is None and result.get("family"):
        audit_path = output / result["family"] / "accessibility.txt"
        if audit_path.is_file():
            text = audit_path.read_text(encoding="utf-8", errors="replace")
    if not text:
        return []
    sections = []
    current_title = "audit"
    current_issues = []
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("audit-"):
            if current_issues or current_title != "audit":
                sections.append((current_title, current_issues))
            current_title = line
            current_issues = []
            continue
        lowered = line.lower()
        if lowered in (
            "no accessibility issues.",
            "no issues found.",
            "none",
            "0 issues",
            "apple erişilebilirlik denetimi uyarı üretmedi.",
        ):
            continue
        current_issues.append(line)
    if current_issues or current_title != "audit":
        sections.append((current_title, current_issues))
    return sections


def _is_advisor_probe(probe):
    journey = probe.get("journey", "").lower()
    screen = probe.get("screen", "").lower()
    category = probe.get("category", "").lower()
    summary = probe.get("summary", "").lower()
    return (
        "literaryadvisor" in journey
        or "danışman" in screen
        or "advisor" in screen
        or "danışman" in category
        or "edebi danışman" in summary
    )


def _group_probes(tagged_probes):
    """Aggregate identical findings across devices/themes so reviewers see concise items."""
    grouped = {}
    for device_label, probe in tagged_probes:
        key = (probe["status"], probe["category"], probe["screen"], probe["summary"], probe["detail"])
        entry = grouped.setdefault(key, {
            "status": probe["status"],
            "category": probe["category"],
            "screen": probe["screen"],
            "summary": probe["summary"],
            "detail": probe["detail"],
            "scopes": [],
        })
        scope = f"{device_label} · {probe['theme']}"
        if scope not in entry["scopes"]:
            entry["scopes"].append(scope)
    return list(grouped.values())


def _format_grouped_probe_lines(grouped_items):
    lines = []
    for item in grouped_items:
        location = " / ".join(part for part in (item["screen"], item["category"]) if part)
        scopes = ", ".join(item["scopes"])
        lines.append(f"- **[{item['status']}] {location}** ({scopes}): {item['summary']}")
        if item["detail"]:
            lines.append(f"  - Detay: {item['detail']}")
    return lines


def build_review_note(results, output):
    status_order = ("FAIL", "WARN", "INFO", "PASS")
    device_probes = [(result, _load_device_probes(result, output)) for result in results]
    all_tagged = [
        (result.get("family") or result.get("name", "Cihaz"), probe)
        for result, probes in device_probes
        for probe in probes
    ]
    total_counts = Counter(probe["status"] for _, probe in all_tagged)

    lines = [
        "# KitapDuy iOS QA — Detaylı İnceleme Notu",
        "",
        "Bu inceleme notu simülatör koşusundaki XCUITest senaryoları, çalışma zamanı denetim probları (`probes.json`), "
        "Apple erişilebilirlik denetimleri (`accessibility.txt`) ve yerel test sunucusu istek kayıtlarından (`fixture_requests`) üretilmiştir.",
        "",
        "## 1. Çalıştırma ve Cihaz Özeti",
        "",
    ]

    if not results:
        lines.append("- Hiçbir simülatör sonucu üretilmedi.")
    for result, probes in device_probes:
        name = result.get("name", "Cihaz")
        family = result.get("family") or name
        runtime = result.get("runtime", "")
        state = "Geçti" if result.get("passed") else "Başarısız"
        seconds = result.get("seconds", "-")
        xc = result.get("xcuitest")
        xc_text = f"{xc['passedTests']}/{xc['totalTestCount']}" if xc else "yok"
        counts = Counter(p["status"] for p in probes)
        audit_sections = _load_accessibility_sections(result, output)
        audit_issue_count = sum(len(issues) for _, issues in audit_sections)
        shots = len([s for s in result.get("screenshots", []) if s != "failure-screen.png"])
        video_state = "Var" if result.get("video") else "Yok"
        probe_text = ", ".join(f"{k}: {counts.get(k, 0)}" for k in status_order)
        runtime_note = f" ({runtime})" if runtime else ""
        lines.append(
            f"- **{family} — {name}{runtime_note}**: **{state}** | Süre: `{seconds}s` | XCUITest: `{xc_text}` | "
            f"Ekran görüntüsü: `{shots}` | Video: `{video_state}` | Problar: `{probe_text}` | Erişilebilirlik bulgusu: `{audit_issue_count}`"
        )
        for warn_key in ("error", "video_warning", "screenshot_warning", "collection_warning"):
            if result.get(warn_key):
                lines.append(f"  - Hata/Uyarı (`{warn_key}`): {result[warn_key]}")

    # Journey outcomes across devices
    outcome_labels = {"passed": "Geçti", "failed": "Hata", "skipped": "Atlandı"}
    combined_outcomes = {}
    for result in results:
        combined_outcomes.update(result.get("journeys", {}))
    if combined_outcomes:
        lines.extend(["", "### Senaryo Matrisi", ""])
        titles = journey_titles(combined_outcomes)
        for test_name, title in titles.items():
            per_dev = []
            for result in results:
                if not result.get("family") and not result.get("journeys"):
                    continue
                fam = result.get("family") or result.get("name", "Cihaz")
                raw_outcome = result.get("journeys", {}).get(test_name)
                per_dev.append(f"{fam}: {outcome_labels.get(raw_outcome, 'Çalıştırılmadı')}")
            lines.append(f"- **{title}** (`{test_name}`): {' | '.join(per_dev)}")

    lines.extend(["", "## 2. Backend / Yerel Test Sunucusu Yazma Doğrulaması (`fixture_requests`)", ""])
    tracked_keys = {key for key, _ in TRACKED_FIXTURE_EVENTS}
    extra_events = sorted({
        event
        for result in results
        for event in result.get("fixture_requests", [])
        if event not in tracked_keys
    })
    for key, description in TRACKED_FIXTURE_EVENTS:
        per_dev = []
        any_hit = False
        for result in results:
            if not result.get("family") and "fixture_requests" not in result:
                continue
            fam = result.get("family") or result.get("name", "Cihaz")
            count = Counter(result.get("fixture_requests", [])).get(key, 0)
            if count > 0:
                any_hit = True
            per_dev.append(f"{fam}: {count}x" if count else f"{fam}: 0")
        mark = "Doğrulandı" if any_hit else "İzlenmedi"
        lines.append(f"- **{mark}** `{key}` — {description} ({' | '.join(per_dev) or 'cihaz verisi yok'})")
    for key in extra_events:
        per_dev = [
            f"{result.get('family') or result.get('name', 'Cihaz')}: {Counter(result.get('fixture_requests', [])).get(key, 0)}x"
            for result in results
        ]
        lines.append(f"- **Ek kayıt** `{key}` ({' | '.join(per_dev)})")

    lines.extend(["", "## 3. Edebi Danışman (`LiteraryAdvisorScreen`) Özel İnceleme Bölümü", ""])
    advisor_tagged = [(dev, probe) for dev, probe in all_tagged if _is_advisor_probe(probe)]
    if not advisor_tagged:
        lines.append(
            "- Bu çalıştırmada Edebi Danışman için çalışma zamanı probu (`probe-*`) kaydedilmedi "
            "(test çalıştırmadan önce hata oluşmuş veya yalnızca özet testler çalışmış olabilir)."
        )
    else:
        advisor_counts = Counter(probe["status"] for _, probe in advisor_tagged)
        lines.append(
            "- Kapsam özeti: " + ", ".join(f"**{k}: {advisor_counts.get(k, 0)}**" for k in status_order)
        )
        lines.append("")
        for bucket, heading in (("FAIL", "### Danışman Hataları (`FAIL`)"),
                                ("WARN", "### Danışman Uyarıları (`WARN`)"),
                                ("INFO", "### Danışman Bilgilendirmeleri (`INFO`)"),
                                ("PASS", "### Danışman Doğrulanan Akışlar (`PASS`)")):
            subset = [(dev, p) for dev, p in advisor_tagged if p["status"] == bucket]
            if subset:
                lines.append(heading)
                lines.extend(_format_grouped_probe_lines(_group_probes(subset)))
                lines.append("")

    lines.extend(["## 4. Öncelikli Bulgular (`FAIL` ve `WARN` — Kod & Arayüz Düzeltme Listesi)", ""])
    fail_tagged = [(dev, p) for dev, p in all_tagged if p["status"] == "FAIL"]
    warn_tagged = [(dev, p) for dev, p in all_tagged if p["status"] == "WARN"]
    if not fail_tagged and not warn_tagged:
        if all_tagged:
            lines.append("- Çalışma zamanı denetim problarında `FAIL` veya `WARN` kaydı üretilmedi.")
        elif any(not r.get("passed") for r in results):
            lines.append("- Çalışma zamanı denetim probları toplanamadı; yukarıdaki test hatalarına ve `uitest.log` günlüklerine bakın.")
        else:
            lines.append("- Çalışma zamanı probları henüz üretilmedi (`probe-*` eklentisi bulunmadı).")
    else:
        if fail_tagged:
            lines.extend(["### Kritik / Kırık Davranışlar (`FAIL`)", ""])
            lines.extend(_format_grouped_probe_lines(_group_probes(fail_tagged)))
            lines.append("")
        if warn_tagged:
            lines.extend(["### Eksikler, Küçük Hedefler ve Uyarılar (`WARN`)", ""])
            lines.extend(_format_grouped_probe_lines(_group_probes(warn_tagged)))
            lines.append("")

    obs_path = output / "observations.txt"
    if obs_path.is_file():
        obs_lines = [line.strip() for line in obs_path.read_text(encoding="utf-8").splitlines() if line.strip()]
        if obs_lines:
            lines.extend(["### Karşılaştırma Gözlemleri (`observations.txt`)", ""])
            lines.extend(f"- {line}" for line in obs_lines)
            lines.append("")

    lines.extend(["## 5. Apple Erişilebilirlik Denetimi (`accessibility.txt`) Özeti", ""])
    any_audit_text = False
    for result in results:
        sections = _load_accessibility_sections(result, output)
        fam = result.get("family") or result.get("name", "Cihaz")
        if not sections:
            lines.append(f"- **{fam}**: Erişilebilirlik denetim çıktısı bulunmadı.")
            continue
        any_audit_text = True
        unique_issues = []
        seen = set()
        total_issues = 0
        for _, issues in sections:
            total_issues += len(issues)
            for issue in issues:
                if issue not in seen:
                    seen.add(issue)
                    unique_issues.append(issue)
        lines.append(
            f"- **{fam}**: `{len(sections)}` ekran denetimi, toplam `{total_issues}` satır bulgu "
            f"(`{len(unique_issues)}` benzersiz). Tam döküm: `{fam}/accessibility.txt`."
        )
        for issue in unique_issues[:25]:
            lines.append(f"  - {issue}")
        if len(unique_issues) > 25:
            lines.append(f"  - ... ve `{len(unique_issues) - 25}` ek benzersiz bulgu (`{fam}/accessibility.txt`).")
    if not any_audit_text and not results:
        lines.append("- Denetim verisi yok.")

    lines.extend(["", "## 6. Doğrulanan Akışlar ve Bilgilendirmeler (`INFO` ve `PASS`)", ""])
    info_tagged = [(dev, p) for dev, p in all_tagged if p["status"] == "INFO"]
    pass_tagged = [(dev, p) for dev, p in all_tagged if p["status"] == "PASS"]
    if info_tagged:
        lines.extend(["### Bilgilendirmeler (`INFO`)", ""])
        lines.extend(_format_grouped_probe_lines(_group_probes(info_tagged)))
        lines.append("")
    if pass_tagged:
        lines.extend([f"### Başarılı Denetimler (`PASS` — {len(_group_probes(pass_tagged))} benzersiz kontrol)", ""])
        lines.extend(_format_grouped_probe_lines(_group_probes(pass_tagged)))
        lines.append("")
    if not info_tagged and not pass_tagged:
        lines.append("- Kaydedilmiş `INFO` veya `PASS` probu yok.")
        lines.append("")

    lines.extend([
        "## 7. Simülatör Kapsam Sınırları ve Kod Tabanında Navigasyondan Erişilemeyen Ekranlar",
        "",
        "- **Yerel test sunucusu kapsamı**: Canlı Supabase oturum açma / Google-Apple OAuth, gerçek StoreKit abonelik satın alma, "
        "anlık bildirim (push) izinleri, fiziksel cihaz ses kesintileri/arka plan davranışı ve canlı üretken yapay zeka yanıtları "
        "bu yerel fixture koşusunda doğrulanmaz.",
        "- **Navigasyonda bağlantısı olmayan (erişilemeyen) Compose ekranları**: Kod tabanında tanımlı olduğu halde "
        "`MainScreen` ve ana gezinme yollarından doğrudan açılamayan ekranlar XCUITest ile gezilemez:",
        "  - " + ", ".join(f"`{screen}`" for screen in UNREACHABLE_CODEBASE_SCREENS),
    ])
    return "\n".join(lines).strip() + "\n"


def _markdown_to_plain_text(md_text):
    out = []
    for line in md_text.splitlines():
        if line.startswith("# "):
            title = line[2:].strip()
            out.append(title)
            out.append("=" * len(title))
        elif line.startswith("## "):
            title = line[3:].strip()
            out.append(f"\n[ {title} ]")
            out.append("-" * (len(title) + 4))
        elif line.startswith("### "):
            out.append(f"\n* {line[4:].strip()}")
        else:
            cleaned = re.sub(r"\*\*(.*?)\*\*", r"\1", line).replace("`", "")
            out.append(cleaned)
    return "\n".join(out).strip() + "\n"


def write_review_note(results, output):
    output.mkdir(parents=True, exist_ok=True)
    note_md = build_review_note(results, output)
    (output / "inceleme-notu.md").write_text(note_md, encoding="utf-8")
    (output / "inceleme-notu.txt").write_text(_markdown_to_plain_text(note_md), encoding="utf-8")
    return note_md


def write_reports(results, output, families=None):
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    review_note = write_review_note(results, output)
    write_gallery(results, output, review_note=review_note, families=families)
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


def write_gallery(results, output, review_note=None, families=None):
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
                                ("accessibility.txt", "Erişilebilirlik bulguları"),
                                ("probes.txt", "Çalışma zamanı denetim notları")):
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
    fallback_families = [family for family in ("iPhone", "iPad") if not families or family in families] or ["iPhone", "iPad"]
    active_families = [family for family in ("iPhone", "iPad") if family in devices] or fallback_families
    groups = []
    family_headers = "".join(f'<th scope="col">{family}</th>' for family in active_families)
    android_header = '<th scope="col">KitapDuy3 · Android</th>' if has_android else ""
    for index, (title, guide, screens) in enumerate(SCREEN_GROUPS, start=1):
        rows = []
        for step, label in screens:
            cells = []
            for family in active_families:
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
                      f'<thead><tr><th scope="col">Ekran</th>{family_headers}{android_header}</tr></thead>'
                      f'<tbody>{"".join(rows)}</tbody></table></section>')

    navigation = " · ".join(
        ['<a href="#review-note">Detaylı İnceleme Notu</a>']
        + [f'<a href="#{family}">{family} sonuçları</a>' for family in devices]
        + [f'<a href="#screens-{index}">{title}</a>'
           for index, (title, _, _) in enumerate(SCREEN_GROUPS, start=1)]
    )
    device_scope = " ve ".join(active_families)
    page = f"""<!doctype html><html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>KitapDuy iOS kullanıcı deneyimi</title><style>
body{{font:16px system-ui;max-width:1080px;margin:32px auto;padding:0 16px;color:#29251f;background:#faf7f1}}
a{{color:#704527}}nav{{line-height:2}}section{{padding:16px 0}}li{{padding-block:3px}}li strong{{margin-inline-start:8px}}
video{{width:min(100%,680px);max-height:460px;background:#221d18}}pre{{white-space:pre-wrap;overflow-wrap:anywhere}}
table{{width:100%;table-layout:fixed;border-collapse:collapse;background:#fff}}th,td{{padding:8px;border:1px solid #dfd6cb;text-align:left}}
th:first-child{{width:24%}}td a{{display:block;margin:6px 0}}img{{display:block;width:100%;height:clamp(110px,36vw,320px);object-fit:contain}}
.review-note-panel{{background:#fff;border:1px solid #dfd6cb;border-radius:10px;padding:16px;margin:16px 0}}
.note-actions{{display:flex;flex-wrap:wrap;gap:12px;align-items:center}}
.copy-btn{{background:#704527;color:#fff;border:0;border-radius:6px;padding:8px 14px;font:600 14px system-ui;cursor:pointer}}
.review-note{{max-height:440px;overflow:auto;background:#faf7f1;border:1px solid #dfd6cb;border-radius:6px;padding:12px;font:13px/1.45 ui-monospace,SFMono-Regular,Menlo,monospace}}
</style><h1>KitapDuy iOS kullanıcı deneyimi</h1>
<p>Gerçek {device_scope} simülatörü görüntülerini KitapDuy3 ile karşılaştırmak için ekranlara tıkla.</p>
<p>Senaryolar yerel örnek veriyle çalışır. Canlı hesap girişi, mağaza işlemleri ve fiziksel cihaz davranışı ayrı doğrulama gerektirir.</p>"""
    reference_info = reference / "source.txt"
    provenance = ""
    if has_android and reference_info.is_file():
        provenance = f'<p>{html.escape(reference_info.read_text(encoding="utf-8", errors="replace").strip())}</p>'
    if review_note is None:
        note_md_path = output / "inceleme-notu.md"
        review_note = note_md_path.read_text(encoding="utf-8") if note_md_path.is_file() else build_review_note(results, output)
    review_section = (
        '<section id="review-note" class="review-note-panel">'
        '<h2>Detaylı İnceleme Notu</h2>'
        '<p>Simülatör çalıştırmasındaki bulgular, Edebi Danışman denetimi ve backend yazma kontrolleri tek raporda toplandı. '
        'Notu panoya kopyalayıp düzeltme turu için doğrudan paylaşabilirsiniz.</p>'
        '<p class="note-actions">'
        '<button type="button" class="copy-btn" id="copy-review-note">Notu Panoya Kopyala</button>'
        '<a href="inceleme-notu.md">inceleme-notu.md</a>'
        '<a href="inceleme-notu.txt">inceleme-notu.txt</a>'
        '<span id="copy-review-status" aria-live="polite"></span>'
        '</p>'
        f'<pre id="review-note-text" class="review-note">{html.escape(review_note)}</pre>'
        '</section>'
    )
    copy_script = """<script>
(function(){
  var btn=document.getElementById('copy-review-note');
  var status=document.getElementById('copy-review-status');
  var source=document.getElementById('review-note-text');
  if(!btn||!source){return;}
  btn.addEventListener('click',function(){
    var text=source.textContent||'';
    function done(ok){if(status){status.textContent=ok?'Panoya kopyalandı.':'Kopyalama başarısız; metni elle seçin.';}}
    if(navigator.clipboard&&navigator.clipboard.writeText){
      navigator.clipboard.writeText(text).then(function(){done(true);},function(){fallback();});
    }else{fallback();}
    function fallback(){
      try{
        var ta=document.createElement('textarea');
        ta.value=text;ta.style.position='fixed';ta.style.opacity='0';
        document.body.appendChild(ta);ta.select();
        done(document.execCommand('copy'));document.body.removeChild(ta);
      }catch(_){done(false);}
    }
  });
})();
</script>"""
    findings_path = output / "observations.txt"
    findings = ""
    if findings_path.is_file():
        observations = findings_path.read_text(encoding="utf-8").splitlines()
        findings = '<section><h2>Karşılaştırma bulguları</h2><ul>' + "".join(
            f'<li>{html.escape(line)}</li>' for line in observations if line.strip()
        ) + '</ul></section>'
    (output / "index.html").write_text(
        page + provenance + f'<nav aria-label="Rapor bölümleri">{navigation}</nav>' +
        review_section + findings + "".join(sections + groups) + copy_script + "</html>",
        encoding="utf-8",
    )


class SimulatorRunner:
    def __init__(self, app, output, duration, url, test_run=None, families=("iPhone", "iPad")):
        self.app, self.output, self.duration, self.url = app, output, duration, url
        self.test_run = test_run
        self.families = tuple(dict.fromkeys(families))
        if not self.families:
            raise ValueError("At least one device family is required.")
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
        commit = os.environ.get("CM_COMMIT") or os.environ.get("GITHUB_SHA") or os.environ.get("GIT_CLONE_COMMIT_HASH")
        if commit:
            result["commit"] = commit
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
                        env=environment, stdout=log, stderr=subprocess.STDOUT, timeout=1500,
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
                    probes_file = directory / "probes.json"
                    if probes_file.is_file():
                        loaded_probes = json.loads(probes_file.read_text(encoding="utf-8"))
                        result["probe_counts"] = dict(Counter(
                            _normalize_probe_record(p)["status"] for p in loaded_probes
                        ))
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
        # Do not stop on the first failure: retain evidence for each selected device family.
        return [self.run_device(device) for device in select_devices(snapshot["devices"], families=self.families)]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", type=Path, required=True)
    parser.add_argument("--test-run", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/ios-smoke"))
    parser.add_argument("--android-reference", type=Path, default=Path("ci/kitapduy3-reference"),
                        help="Directory of Android reference PNGs named after report steps.")
    parser.add_argument("--duration", type=int, default=125)
    parser.add_argument("--devices", default=(os.environ.get("IOS_SMOKE_DEVICES") or "both"),
                        help="Device families to test: both, iphone, or ipad (or IOS_SMOKE_DEVICES).")
    parser.add_argument("--https-url", default=os.environ.get("IOS_SMOKE_HTTPS_URL", "https://www.apple.com/library/test/success.html"))
    args = parser.parse_args(argv)
    if args.duration < 1:
        parser.error("--duration must be positive")
    try:
        families = parse_device_families(args.devices)
    except ValueError as error:
        parser.error(str(error))
    output = args.output.resolve()
    try:
        url = urlsplit(args.https_url)
        if url.scheme != "https" or not url.hostname or url.username or url.password:
            raise ValueError("Use an HTTPS endpoint without embedded credentials.")
        if not args.test_run.is_file():
            raise ValueError("The Xcode test configuration (.xctestrun) is missing.")
        results = SimulatorRunner(
            args.app.resolve(),
            output,
            args.duration,
            args.https_url,
            args.test_run.resolve(),
            families=families,
        ).run()
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
    write_reports(results, output, families=families)
    return 0 if len(results) == len(families) and all(result["passed"] for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
