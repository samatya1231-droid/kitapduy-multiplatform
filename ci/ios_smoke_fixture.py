"""Loopback-only sample data for the simulator UI tests."""

import base64
from contextlib import contextmanager
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import re
from threading import Lock, Thread
import time
from urllib.parse import parse_qs, urlsplit

USER_ID = "b408ace6-91d0-4c5d-98cc-001234567890"
BOOK_ID = "local_ci_book"
PODCAST_ID = "ci_podcast"


def test_token():
    def encode(data):
        return base64.urlsafe_b64encode(json.dumps(data).encode()).decode().rstrip("=")
    header = encode({"alg": "HS256", "typ": "JWT"})
    claims = encode({"sub": USER_ID, "role": "authenticated", "exp": int(time.time()) + 3600})
    return f"{header}.{claims}.c2ltdWxhdG9y"


def normalize_tr(value):
    return (
        str(value)
        .replace("İ", "i")
        .replace("I", "i")
        .replace("ı", "i")
        .replace("Ç", "c")
        .replace("ç", "c")
        .replace("Ğ", "g")
        .replace("ğ", "g")
        .replace("Ö", "o")
        .replace("ö", "o")
        .replace("Ş", "s")
        .replace("ş", "s")
        .replace("Ü", "u")
        .replace("ü", "u")
        .replace("Â", "a")
        .replace("â", "a")
        .casefold()
    )


def _resolve_cover_name(book_id, category, tags, available_covers):
    top_name = f"top_{book_id}.jpg"
    if top_name in available_covers:
        return top_name
    for raw in [category] + list(tags or []):
        c = normalize_tr(raw)
        if "polisiye" in c and "cat_polisiye_kizil_dosya.jpg" in available_covers:
            return "cat_polisiye_kizil_dosya.jpg"
        if "roman" in c and "cat_roman_budala.jpg" in available_covers:
            return "cat_roman_budala.jpg"
        if "tarih" in c and "cat_tarih_lincoln.jpg" in available_covers:
            return "cat_tarih_lincoln.jpg"
        if "felsefe" in c and "cat_felsefe_schopenhauer.jpg" in available_covers:
            return "cat_felsefe_schopenhauer.jpg"
        if "bilim kurgu" in c and "cat_bilimkurgu_frankenstein.jpg" in available_covers:
            return "cat_bilimkurgu_frankenstein.jpg"
        if "siir" in c and "cat_siir_siirler.jpg" in available_covers:
            return "cat_siir_siirler.jpg"
        if "kisisel" in c and "cat_gelisim_kendime.jpg" in available_covers:
            return "cat_gelisim_kendime.jpg"
        if "cocuk" in c and "cat_cocuk_ozal.jpg" in available_covers:
            return "cat_cocuk_ozal.jpg"
        if "turk" in c and "cat_turk_kandil.jpg" in available_covers:
            return "cat_turk_kandil.jpg"
        if "ingilizce" in c and "cat_ingilizce_sherlock.jpg" in available_covers:
            return "cat_ingilizce_sherlock.jpg"
        if "klasik" in c and "cat_klasik_budala.jpg" in available_covers:
            return "cat_klasik_budala.jpg"
    if "cat_dunya_demir_akce.jpg" in available_covers:
        return "cat_dunya_demir_akce.jpg"
    return next(iter(sorted(available_covers)))


def _load_catalog_templates(available_covers):
    catalog_path = Path(__file__).with_name("catalog_books.json")
    raw_books = json.loads(catalog_path.read_text(encoding="utf-8"))
    templates = [
        {
            "id": BOOK_ID,
            "title": "KitapDuy Deneme Kitabı",
            "author": "Deneme Yazarı",
            "category": "Roman",
            "tags": ["Roman"],
            "cover_file": "dorian_gray.jpg" if "dorian_gray.jpg" in available_covers else _resolve_cover_name(BOOK_ID, "Roman", [], available_covers),
            "description": "Simülatör için örnek sesli kitap.",
            "duration": 90_000,
            "view_count": 10,
            "like_count": 1,
            "language": "tr",
        }
    ]
    for item in raw_books:
        book_id = item.get("id", "")
        if not book_id or book_id == BOOK_ID:
            continue
        category = item.get("category") or "Dünya Edebiyatı"
        tags = item.get("tags") or []
        templates.append({
            "id": book_id,
            "title": item.get("title", ""),
            "author": item.get("author", ""),
            "category": category,
            "tags": tags,
            "cover_file": _resolve_cover_name(book_id, category, tags, available_covers),
            "description": item.get("description", ""),
            "duration": item.get("duration", 0),
            "view_count": item.get("view_count", 0),
            "like_count": item.get("like_count", 0),
            "language": item.get("language", "tr"),
        })
    return templates


@contextmanager
def serve_fixture():
    # A generated 90-second tone exercises the same HTTP MP3 path as the catalog.
    audio = Path(__file__).with_name("ios-smoke-sample.mp3").read_bytes()
    covers_dir = Path(__file__).with_name("covers")
    covers = {
        cover_path.name: cover_path.read_bytes()
        for cover_path in sorted(covers_dir.glob("*.jpg"))
        if cover_path.is_file()
    }
    catalog_templates = _load_catalog_templates(set(covers.keys()))
    cover = covers["sherlock_gloria.jpg"]
    profile = {"uid": USER_ID, "name": "Deneme Okuru"}
    community_users = [
        {
            "uid": "community_donor_1",
            "name": "Elif Yılmaz",
            "username": "elif_okur",
            "bio": "Klasikler ve denemeler",
            "is_premium": True,
        },
        {
            "uid": "community_donor_2",
            "name": "Mert Kaya",
            "username": "mert_okur",
            "bio": "Bilim ve sesli kitap meraklısı",
            "is_premium": False,
        },
    ]
    posts = []
    feed_comments = []
    user_bookshelves = []
    favorites = []
    followers = []
    reports = []
    blocked_users = []
    askida_books = [
        {
            "id": "askida_seed_1",
            "book_name": "Kürk Mantolu Madonna",
            "donor_id": "community_donor_1",
            "donor_name": "Elif Yılmaz",
            "donor_note": "Bir başka okurun kalbine dokunması dileğiyle, temiz durumda.",
            "cover_file": "top_lX29B2MpxEshEZY0ihlA.jpg",
            "status": "AVAILABLE",
            "claimer_id": None,
            "city": "İstanbul",
            "category": "Roman",
            "condition": "Çok İyi",
            "timestamp": 1_730_000_000_000,
        },
        {
            "id": "askida_seed_2",
            "book_name": "Küçük Prens",
            "donor_id": "community_donor_2",
            "donor_name": "Mert Kaya",
            "donor_note": "Çocuklar ve içindeki çocuğu yaşatanlar için keyifle okunsun.",
            "cover_file": "kucuk_prens.jpg",
            "status": "AVAILABLE",
            "claimer_id": None,
            "city": "Ankara",
            "category": "Klasik",
            "condition": "Yeni Gibi",
            "timestamp": 1_729_900_000_000,
        },
        {
            "id": "askida_seed_3",
            "book_name": "Suç ve Ceza",
            "donor_id": "community_donor_3",
            "donor_name": "Zeynep Demir",
            "donor_note": "Altını çizmeden özenle okudum, sıradaki okuruna emanet.",
            "cover_file": "top_DXRmYsBl10qfocIjJQYj.jpg",
            "status": "AVAILABLE",
            "claimer_id": None,
            "city": "İzmir",
            "category": "Dünya Edebiyatı",
            "condition": "İyi",
            "timestamp": 1_729_800_000_000,
        },
    ]
    askida_messages = []
    quiz_leaderboard = [
        {"user_id": "seed_1", "user_name": "Elif Yılmaz", "total_score": 4200, "updated_at": None},
        {"user_id": "seed_2", "user_name": "Ahmet Kaya", "total_score": 3650, "updated_at": None},
        {"user_id": "seed_3", "user_name": "Zeynep Demir", "total_score": 2900, "updated_at": None},
        {"user_id": "seed_4", "user_name": "Murat Şahin", "total_score": 2150, "updated_at": None},
        {"user_id": "seed_5", "user_name": "Selin Aydın", "total_score": 1600, "updated_at": None},
    ]
    requests = []
    state_lock = Lock()
    now = datetime.now(timezone.utc).isoformat()
    notifications = [
        {"id": 1, "receiver_id": USER_ID, "message": "Deneme bildirimi", "is_read": False,
         "created_at": now, "type": "book", "book_id": BOOK_ID}
    ]
    id_counters = {
        "posts": 0,
        "feed_comments": 0,
        "user_bookshelves": 0,
        "favorites": 0,
        "notifications": 1,
        "reports": 0,
    }

    control_params = {"select", "order", "limit", "offset", "columns", "on_conflict", "or"}

    def next_id(table_name):
        id_counters[table_name] = id_counters.get(table_name, 0) + 1
        return id_counters[table_name]

    def eq_match(actual, target):
        if isinstance(actual, bool):
            return ("true" if actual else "false") == str(target).lower()
        if actual is None:
            return False
        return str(actual) == str(target)

    def matches_filters(row, query):
        for field, values in query.items():
            if field in control_params:
                continue
            for condition in values:
                if condition.startswith("eq."):
                    if not eq_match(row.get(field), condition[3:]):
                        return False
                elif condition.startswith("neq."):
                    if eq_match(row.get(field), condition[4:]):
                        return False
                elif condition.startswith("ilike."):
                    needle = normalize_tr(condition[6:].strip("%*"))
                    if needle not in normalize_tr(row.get(field, "")):
                        return False
                elif condition.startswith("in.(") and condition.endswith(")"):
                    raw_items = [item.strip().strip('"').strip("'") for item in condition[4:-1].split(",") if item.strip()]
                    if not any(eq_match(row.get(field), item) for item in raw_items):
                        return False
                elif condition.startswith("is."):
                    expected = condition[3:].lower()
                    actual = row.get(field)
                    if expected == "null" and actual is not None:
                        return False
                    if expected == "true" and actual is not True:
                        return False
                    if expected == "false" and actual is not False:
                        return False
        if "or" in query:
            terms = re.findall(r"(\w+)\.(ilike|eq|neq)\.([^,)]+)", query["or"][0])
            if terms:
                def matches_term(key, op, val):
                    if op == "eq":
                        return eq_match(row.get(key), val)
                    if op == "neq":
                        return not eq_match(row.get(key), val)
                    return normalize_tr(val.strip("%*")) in normalize_tr(row.get(key, ""))
                if not any(matches_term(key, op, value) for key, op, value in terms):
                    return False
        return True

    def sorted_rows(rows, query):
        order_expr = (query.get("order") or [""])[0]
        if not order_expr:
            return rows
        first = order_expr.split(",")[0].strip()
        if not first:
            return rows
        parts = first.split(".")
        key = parts[0]
        desc = len(parts) > 1 and parts[1].lower() == "desc"
        return sorted(
            rows,
            key=lambda r: (
                r.get(key) is not None,
                0 if isinstance(r.get(key), (int, float)) else 1,
                r.get(key) if isinstance(r.get(key), (int, float)) else str(r.get(key, "")),
            ),
            reverse=desc,
        )

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def send_data(self, status, data, mime="application/json", headers=None):
            if mime == "application/json" and not isinstance(data, (bytes, bytearray)):
                data = json.dumps(data, ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header("Content-Type", mime)
            self.send_header("Content-Length", str(len(data)))
            for name, value in (headers or {}).items():
                self.send_header(name, value)
            self.end_headers()
            if self.command != "HEAD":
                try:
                    self.wfile.write(data)
                except (BrokenPipeError, ConnectionResetError):
                    pass

        def do_HEAD(self):
            self.do_GET()

        def do_GET(self):
            parsed = urlsplit(self.path)
            path = parsed.path
            query = parse_qs(parsed.query)
            base = f"http://localhost:{self.server.server_port}"
            if path == "/audio.mp3":
                # AVPlayer probes and seeks using byte ranges.
                match = re.fullmatch(r"bytes=(\d+)-(\d*)", self.headers.get("Range", ""))
                headers = {"Accept-Ranges": "bytes"}
                if match:
                    start = int(match[1])
                    end = min(int(match[2]) if match[2] else len(audio) - 1, len(audio) - 1)
                    if start > end:
                        self.send_data(416, b"", "audio/mpeg", {"Content-Range": f"bytes */{len(audio)}"})
                        return
                    headers["Content-Range"] = f"bytes {start}-{end}/{len(audio)}"
                    self.send_data(206, audio[start:end + 1], "audio/mpeg", headers)
                else:
                    self.send_data(200, audio, "audio/mpeg", headers)
                return
            if path == "/cover.png":
                self.send_data(200, cover, "image/jpeg")
                return
            if path.startswith("/covers/"):
                name = path[len("/covers/"):]
                if "/" in name or "\\" in name or ".." in name or name not in covers:
                    self.send_data(404, {"message": "Unknown cover"})
                else:
                    self.send_data(200, covers[name], "image/jpeg")
                return
            if path == "/podcast.xml":
                feed = query.get("feed", [""])[0]
                if feed == "111hz":
                    rss = f"""<rss version="2.0"><channel><title>Barış Özcan ile 111 Hz</title>
                    <item><title>Yaratıcılık ve Teknoloji</title><description>Bilim, sanat ve tasarım üzerine sohbetler.</description>
                    <enclosure url="{base}/audio.mp3?episode=1" type="audio/mpeg" length="{len(audio)}"/>
                    <duration>00:24:10</duration></item>
                    <item><title>Geleceğin Okuma Kültürü</title><description>Sesli kitaplar ve dijital hafıza.</description>
                    <enclosure url="{base}/audio.mp3?episode=2" type="audio/mpeg" length="{len(audio)}"/>
                    <duration>00:18:45</duration></item></channel></rss>"""
                elif feed == "kuran":
                    rss = f"""<rss version="2.0"><channel><title>Kur'an-ı Kerim Türkçe Meali</title>
                    <item><title>Fatiha ve Bakara Seçkisi</title><description>Anlam odaklı sesli meal dinletisi.</description>
                    <enclosure url="{base}/audio.mp3?episode=1" type="audio/mpeg" length="{len(audio)}"/>
                    <duration>00:15:30</duration></item></channel></rss>"""
                elif feed == "hayatta":
                    rss = f"""<rss version="2.0"><channel><title>Hayatta Bir Gün</title>
                    <item><title>Şehir ve Hafıza</title><description>Günlük yaşamdan edebiyat ve kültür durakları.</description>
                    <enclosure url="{base}/audio.mp3?episode=1" type="audio/mpeg" length="{len(audio)}"/>
                    <duration>00:21:00</duration></item></channel></rss>"""
                else:
                    rss = f"""<rss version="2.0"><channel><title>KitapDuy Deneme Podcasti</title>
                    <item><title>Deneme Bölümü</title><description>Simülatör için örnek bölüm.</description>
                    <enclosure url="{base}/audio.mp3?episode=1" type="audio/mpeg" length="{len(audio)}"/>
                    <duration>00:01:30</duration></item></channel></rss>"""
                self.send_data(200, rss.encode(), "application/rss+xml")
                return
            if not path.startswith("/rest/v1/"):
                self.send_data(404, {"message": "Unknown fixture route"})
                return
            table = path.rsplit("/", 1)[1]
            with state_lock:
                if table == "books":
                    rows = [
                        {
                            "id": item["id"],
                            "title": item["title"],
                            "author": item["author"],
                            "category": item["category"],
                            "tags": item["tags"],
                            "cover_url": f"{base}/covers/{item['cover_file']}",
                            "audio_url": f"{base}/audio.mp3",
                            "description": item["description"],
                            "duration": item["duration"],
                            "view_count": item["view_count"],
                            "like_count": item["like_count"],
                            "language": item["language"],
                        }
                        for item in catalog_templates
                    ]
                elif table == "podcasts":
                    rows = [
                        {"id": "podcast_111hz", "name": "Barış Özcan ile 111 Hz", "author": "Barış Özcan",
                         "rss_url": f"{base}/podcast.xml?feed=111hz", "image_url": f"{base}/covers/podcast_111hz.jpg", "category": "Bilim"},
                        {"id": "podcast_kuran", "name": "Kur'an-ı Kerim Türkçe Meali", "author": "Sesli Meal",
                         "rss_url": f"{base}/podcast.xml?feed=kuran", "image_url": f"{base}/covers/podcast_kuran.jpg", "category": "Din"},
                        {"id": "podcast_hayatta_bir_gun", "name": "Hayatta Bir Gün", "author": "Kültür & Sohbet",
                         "rss_url": f"{base}/podcast.xml?feed=hayatta", "image_url": f"{base}/covers/podcast_hayatta_bir_gun.jpg", "category": "Kültür"},
                        {"id": PODCAST_ID, "name": "KitapDuy Deneme Podcasti", "author": "Deneme Yapımı",
                         "rss_url": f"{base}/podcast.xml", "image_url": f"{base}/covers/podcast_111hz.jpg", "category": "Roman"},
                    ]
                elif table == "askida_kitap":
                    rows = []
                    for item in askida_books:
                        cover_file = item.get("cover_file")
                        image_url = item.get("image_url") or (f"{base}/covers/{cover_file}" if cover_file in covers else "")
                        rows.append({**{k: v for k, v in item.items() if k != "cover_file"}, "image_url": image_url})
                elif table == "askida_mesaj":
                    rows = [dict(msg) for msg in askida_messages]
                elif table == "quiz_leaderboard":
                    rows = sorted(
                        (dict(entry) for entry in quiz_leaderboard),
                        key=lambda entry: entry.get("total_score", 0),
                        reverse=True,
                    )
                elif table == "users":
                    rows = [dict(profile), *(dict(u) for u in community_users)]
                elif table == "posts":
                    rows = [dict(post) for post in reversed(posts)]
                elif table == "feed_comments":
                    rows = [dict(comment) for comment in reversed(feed_comments)]
                elif table == "user_bookshelves":
                    rows = [dict(item) for item in reversed(user_bookshelves)]
                elif table == "favorites":
                    rows = [dict(item) for item in favorites]
                elif table == "followers":
                    rows = [dict(item) for item in followers]
                elif table == "notifications":
                    rows = [dict(item) for item in notifications]
                elif table == "reports":
                    rows = [dict(item) for item in reports]
                elif table == "blocked_users":
                    rows = [dict(item) for item in blocked_users]
                else:
                    rows = []
            # Support PostgREST filters (eq, neq, ilike, in, is, or), ordering, range & exact counts.
            rows = [row for row in rows if matches_filters(row, query)]
            rows = sorted_rows(rows, query)
            total_count = len(rows)
            range_header = self.headers.get("Range", "")
            range_match = re.fullmatch(r"(\d+)-(\d*)", range_header.strip())
            if range_match:
                start = int(range_match[1])
                end = int(range_match[2]) if range_match[2] else len(rows) - 1
                rows = rows[start:end + 1]
            else:
                offset_val = int(query["offset"][0]) if "offset" in query and query["offset"][0].isdigit() else 0
                limit_val = int(query["limit"][0]) if "limit" in query and query["limit"][0].isdigit() else None
                if offset_val or limit_val is not None:
                    rows = rows[offset_val:(offset_val + limit_val) if limit_val is not None else None]

            resp_headers = {}
            prefer = self.headers.get("Prefer", "")
            if self.command == "HEAD" or "count=exact" in prefer:
                resp_headers["Content-Range"] = f"0-{len(rows) - 1}/{total_count}" if rows else f"*/{total_count}"
            if self.command == "HEAD":
                self.send_data(200, b"", "application/json", resp_headers)
                return
            if "application/vnd.pgrst.object+json" in self.headers.get("Accept", ""):
                if len(rows) != 1:
                    self.send_data(406, {"code": "PGRST116", "message": "No matching fixture row"}, headers=resp_headers)
                    return
                self.send_data(200, rows[0], headers=resp_headers)
            else:
                self.send_data(200, rows, headers=resp_headers)

        def wants_representation(self, query):
            prefer = self.headers.get("Prefer", "")
            return "return=representation" in prefer or "select" in query

        def send_write_response(self, empty_status, rep_status, affected_rows, query):
            if not self.wants_representation(query):
                self.send_data(empty_status, b"", "text/plain")
                return
            if "application/vnd.pgrst.object+json" in self.headers.get("Accept", ""):
                if len(affected_rows) != 1:
                    self.send_data(406, {"code": "PGRST116", "message": "No matching fixture row"})
                    return
                self.send_data(rep_status, affected_rows[0])
            else:
                self.send_data(rep_status, affected_rows)

        def do_POST(self):
            path = urlsplit(self.path).path
            body = self.read_body()
            if body is None:
                self.send_data(400, {"message": "Invalid JSON body"})
                return
            if path in ("/auth/v1/signup", "/auth/v1/token"):
                if path.endswith("/signup"):
                    with state_lock:
                        requests.append("guest_signup")
                user = {"id": USER_ID, "aud": "authenticated", "role": "authenticated", "created_at": now,
                        "updated_at": now, "is_anonymous": True, "app_metadata": {}, "user_metadata": {}}
                self.send_data(200, {"access_token": test_token(), "token_type": "bearer", "expires_in": 3600,
                                     "refresh_token": "simulator-fixture", "user": user})
            else:
                self.insert_rows(path, body)

        def insert_rows(self, path, body):
            allowed = (
                "/rest/v1/users",
                "/rest/v1/posts",
                "/rest/v1/feed_comments",
                "/rest/v1/user_bookshelves",
                "/rest/v1/favorites",
                "/rest/v1/followers",
                "/rest/v1/notifications",
                "/rest/v1/reports",
                "/rest/v1/blocked_users",
                "/rest/v1/askida_kitap",
                "/rest/v1/askida_mesaj",
                "/rest/v1/quiz_leaderboard",
            )
            if path not in allowed:
                self.send_data(404, {"message": "Unknown fixture write"})
                return
            # Supabase sends an array even when inserting or upserting one row.
            rows = body if isinstance(body, list) else [body]
            if not rows or not all(isinstance(row, dict) for row in rows):
                self.send_data(400, {"message": "Expected a JSON object or array of objects"})
                return
            if path == "/rest/v1/users" and (len(rows) != 1 or rows[0].get("uid", USER_ID) != USER_ID):
                self.send_data(400, {"message": "Expected one row for the fixture user"})
                return
            query = parse_qs(urlsplit(self.path).query)
            created = []
            with state_lock:
                if path == "/rest/v1/users":
                    profile.update(rows[0])
                    created = [dict(profile)]
                elif path == "/rest/v1/posts":
                    for row in rows:
                        stored = {
                            "likes": 0,
                            "liked_by": "",
                            "comment_count": 0,
                            "poll_votes": {},
                            "poll_voters": [],
                            **row,
                            "id": next_id("posts"),
                            "created_at": now,
                        }
                        posts.append(stored)
                        created.append(dict(stored))
                    requests.append("create_post")
                elif path == "/rest/v1/feed_comments":
                    for row in rows:
                        stored = {
                            "likes": 0,
                            "liked_by": "",
                            **row,
                            "id": next_id("feed_comments"),
                            "created_at": now,
                        }
                        feed_comments.append(stored)
                        created.append(dict(stored))
                    requests.append("create_comment")
                elif path == "/rest/v1/user_bookshelves":
                    for row in rows:
                        stored = {**row, "id": next_id("user_bookshelves"), "created_at": now}
                        user_bookshelves.append(stored)
                        created.append(dict(stored))
                    requests.append("add_shelf")
                elif path == "/rest/v1/favorites":
                    for row in rows:
                        stored = {**row, "id": next_id("favorites"), "created_at": now}
                        favorites.append(stored)
                        created.append(dict(stored))
                elif path == "/rest/v1/followers":
                    for row in rows:
                        follower_id = row.get("follower_id")
                        following_id = row.get("following_id")
                        if not any(f.get("follower_id") == follower_id and f.get("following_id") == following_id for f in followers):
                            stored = {**row, "created_at": now}
                            followers.append(stored)
                            created.append(dict(stored))
                        else:
                            created.append(dict(row))
                    requests.append("follow_user")
                elif path == "/rest/v1/notifications":
                    for row in rows:
                        stored = {"is_read": False, **row, "id": next_id("notifications"), "created_at": now}
                        notifications.append(stored)
                        created.append(dict(stored))
                elif path == "/rest/v1/reports":
                    for row in rows:
                        stored = {**row, "id": next_id("reports"), "created_at": now}
                        reports.append(stored)
                        created.append(dict(stored))
                elif path == "/rest/v1/blocked_users":
                    for row in rows:
                        stored = {**row, "created_at": now}
                        blocked_users.append(stored)
                        created.append(dict(stored))
                elif path == "/rest/v1/askida_kitap":
                    for row in rows:
                        stored = dict(row)
                        askida_books.insert(0, stored)
                        created.append(dict(stored))
                    requests.append("donate_askida")
                elif path == "/rest/v1/askida_mesaj":
                    for row in rows:
                        stored = dict(row)
                        askida_messages.append(stored)
                        created.append(dict(stored))
                    requests.append("send_askida_chat")
                elif path == "/rest/v1/quiz_leaderboard":
                    for row in rows:
                        uid = row.get("user_id")
                        existing = next((e for e in quiz_leaderboard if e.get("user_id") == uid), None)
                        if existing is not None:
                            existing.update(row)
                            created.append(dict(existing))
                        else:
                            stored = dict(row)
                            quiz_leaderboard.append(stored)
                            created.append(dict(stored))
            self.send_write_response(201, 201, created, query)

        def do_PATCH(self):
            parsed = urlsplit(self.path)
            allowed = (
                "/rest/v1/users",
                "/rest/v1/posts",
                "/rest/v1/feed_comments",
                "/rest/v1/notifications",
                "/rest/v1/askida_kitap",
                "/rest/v1/quiz_leaderboard",
            )
            if parsed.path not in allowed:
                self.send_data(404, {"message": "Unknown fixture update"})
                return
            body = self.read_body()
            if not isinstance(body, dict):
                self.send_data(400, {"message": "Expected a JSON object"})
                return
            query = parse_qs(parsed.query)
            updated = []
            with state_lock:
                if parsed.path == "/rest/v1/users":
                    if matches_filters(profile, query):
                        profile.update(body)
                        updated.append(dict(profile))
                        requests.append("update_profile")
                elif parsed.path == "/rest/v1/posts":
                    for post in posts:
                        if matches_filters(post, query):
                            post.update(body)
                            updated.append(dict(post))
                    if updated:
                        requests.append("update_post")
                elif parsed.path == "/rest/v1/feed_comments":
                    for comment in feed_comments:
                        if matches_filters(comment, query):
                            comment.update(body)
                            updated.append(dict(comment))
                elif parsed.path == "/rest/v1/notifications":
                    for note in notifications:
                        if matches_filters(note, query):
                            note.update(body)
                            updated.append(dict(note))
                    if updated and body.get("is_read") is True:
                        requests.append("mark_notifications_read")
                elif parsed.path == "/rest/v1/askida_kitap":
                    for item in askida_books:
                        if matches_filters(item, query):
                            item.update(body)
                            updated.append(dict(item))
                elif parsed.path == "/rest/v1/quiz_leaderboard":
                    for entry in quiz_leaderboard:
                        if matches_filters(entry, query):
                            entry.update(body)
                            updated.append(dict(entry))
            self.send_write_response(204, 200, updated, query)

        def do_DELETE(self):
            parsed = urlsplit(self.path)
            table_map = {
                "/rest/v1/posts": posts,
                "/rest/v1/feed_comments": feed_comments,
                "/rest/v1/followers": followers,
                "/rest/v1/user_bookshelves": user_bookshelves,
                "/rest/v1/favorites": favorites,
            }
            if parsed.path not in table_map:
                self.send_data(404, {"message": "Unknown fixture delete"})
                return
            query = parse_qs(parsed.query)
            target_list = table_map[parsed.path]
            deleted = []
            kept = []
            with state_lock:
                for row in target_list:
                    if matches_filters(row, query):
                        deleted.append(dict(row))
                    else:
                        kept.append(row)
                target_list[:] = kept
                if parsed.path == "/rest/v1/posts" and deleted:
                    requests.append("delete_post")
                elif parsed.path == "/rest/v1/feed_comments" and deleted:
                    requests.append("delete_comment")
                elif parsed.path == "/rest/v1/followers" and deleted:
                    requests.append("unfollow_user")
            self.send_write_response(204, 200, deleted, query)

        def read_body(self):
            try:
                return json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or "{}")
            except ValueError:
                return None

    with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            yield f"http://localhost:{server.server_port}", requests
        finally:
            server.shutdown()
            thread.join()
