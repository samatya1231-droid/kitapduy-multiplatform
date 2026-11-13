"""Loopback-only sample data for the simulator UI tests."""

import base64
from contextlib import contextmanager
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import io
import json
from pathlib import Path
import re
from threading import Lock, Thread
import time
from urllib.parse import parse_qs, urlsplit
import wave

USER_ID = "b408ace6-91d0-4c5d-98cc-001234567890"
BOOK_ID = "local_ci_book"
PODCAST_ID = "ci_podcast"


def audio_sample():
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as audio:
        audio.setparams((1, 2, 8000, 0, "NONE", "not compressed"))
        # Long enough to test forward/back controls; no copyrighted recording.
        audio.writeframes(bytes(8000 * 2 * 90))
    return buffer.getvalue()


def test_token():
    def encode(data):
        return base64.urlsafe_b64encode(json.dumps(data).encode()).decode().rstrip("=")
    header = encode({"alg": "HS256", "typ": "JWT"})
    claims = encode({"sub": USER_ID, "role": "authenticated", "exp": int(time.time()) + 3600})
    return f"{header}.{claims}.c2ltdWxhdG9y"


@contextmanager
def serve_fixture():
    audio = audio_sample()
    cover = (Path(__file__).resolve().parents[1] / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon_1024.png").read_bytes()
    profile = {"uid": USER_ID, "name": "Deneme Okuru"}
    posts = []
    requests = []
    state_lock = Lock()
    now = datetime.now(timezone.utc).isoformat()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def send_data(self, status, data, mime="application/json", headers=None):
            if mime == "application/json":
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
            if path == "/audio.wav":
                # AVPlayer probes and seeks using byte ranges.
                match = re.fullmatch(r"bytes=(\d+)-(\d*)", self.headers.get("Range", ""))
                headers = {"Accept-Ranges": "bytes"}
                if match:
                    start = int(match[1])
                    end = min(int(match[2]) if match[2] else len(audio) - 1, len(audio) - 1)
                    if start > end:
                        self.send_data(416, b"", "audio/wav", {"Content-Range": f"bytes */{len(audio)}"})
                        return
                    headers["Content-Range"] = f"bytes {start}-{end}/{len(audio)}"
                    self.send_data(206, audio[start:end + 1], "audio/wav", headers)
                else:
                    self.send_data(200, audio, "audio/wav", headers)
                return
            if path == "/cover.png":
                self.send_data(200, cover, "image/png")
                return
            if path == "/podcast.xml":
                rss = f"""<rss version="2.0"><channel><title>KitapDuy Deneme Podcasti</title>
                <item><title>Deneme Bölümü</title><description>Simülatör için örnek bölüm.</description>
                <enclosure url="{base}/audio.wav?episode=1" type="audio/wav" length="{len(audio)}"/>
                <duration>00:01:30</duration></item></channel></rss>"""
                self.send_data(200, rss.encode(), "application/rss+xml")
                return
            if not path.startswith("/rest/v1/"):
                self.send_data(404, {"message": "Unknown fixture route"})
                return
            table = path.rsplit("/", 1)[1]
            with state_lock:
                if table == "books":
                    rows = [{"id": BOOK_ID, "title": "KitapDuy Deneme Kitabı", "author": "Deneme Yazarı",
                             "category": "Roman", "cover_url": f"{base}/cover.png", "audio_url": f"{base}/audio.wav",
                             "description": "Simülatör için örnek sesli kitap.", "duration": 90_000}]
                elif table == "podcasts":
                    rows = [{"id": PODCAST_ID, "name": "KitapDuy Deneme Podcasti", "author": "Deneme Yapımı",
                             "rss_url": f"{base}/podcast.xml", "image_url": f"{base}/cover.png", "category": "Roman"}]
                elif table == "users":
                    rows = [dict(profile)]
                elif table == "posts":
                    rows = [dict(post) for post in reversed(posts)]
                elif table == "notifications":
                    rows = [{"id": 1, "receiver_id": USER_ID, "message": "Deneme bildirimi", "is_read": False,
                             "created_at": now, "type": "book", "book_id": BOOK_ID}]
                else:
                    rows = []
            # Support the PostgREST equality and search filters used by these screens.
            for field, values in query.items():
                condition = values[0]
                if condition.startswith("eq."):
                    rows = [row for row in rows if str(row.get(field, "")) == condition[3:]]
            if "or" in query:
                terms = re.findall(r"(\w+)\.ilike\.([^,)]+)", query["or"][0])
                if terms:
                    rows = [row for row in rows if any(value.strip("%*").casefold() in str(row.get(key, "")).casefold()
                                                       for key, value in terms)]
            if "application/vnd.pgrst.object+json" in self.headers.get("Accept", ""):
                if len(rows) != 1:
                    self.send_data(406, {"code": "PGRST116", "message": "No matching fixture row"})
                    return
                self.send_data(200, rows[0])
            else:
                self.send_data(200, rows)

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
            if path not in ("/rest/v1/users", "/rest/v1/posts"):
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
            with state_lock:
                if path == "/rest/v1/users":
                    profile.update(rows[0])
                else:
                    for row in rows:
                        posts.append({**row, "id": len(posts) + 1, "created_at": now})
                    requests.append("create_post")
            self.send_data(201, b"", "text/plain")

        def do_PATCH(self):
            parsed = urlsplit(self.path)
            if parsed.path != "/rest/v1/users":
                self.send_data(404, {"message": "Unknown fixture update"})
                return
            body = self.read_body()
            if not isinstance(body, dict):
                self.send_data(400, {"message": "Expected a JSON object"})
                return
            uid_filter = parse_qs(parsed.query).get("uid")
            if uid_filter is None or uid_filter == [f"eq.{USER_ID}"]:
                with state_lock:
                    profile.update(body)
                    requests.append("update_profile")
            self.send_data(204, b"", "text/plain")

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
