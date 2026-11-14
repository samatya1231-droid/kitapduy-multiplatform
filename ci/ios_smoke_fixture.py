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
    cover = covers["sherlock_gloria.jpg"]
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
                        {"id": BOOK_ID, "title": "KitapDuy Deneme Kitabı", "author": "Deneme Yazarı",
                         "category": "Roman", "cover_url": f"{base}/covers/dorian_gray.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Simülatör için örnek sesli kitap.", "duration": 90_000, "view_count": 10},
                        {"id": "sample_roman_alman_gocmen", "title": "Alman Göçmen", "author": "Jules Verne",
                         "category": "Roman", "cover_url": f"{base}/covers/alman_gocmen.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Bilinmeyen kıtalara uzanan klasik macera romanı.", "duration": 5_400_000, "view_count": 1890},
                        {"id": "sample_roman_dort_hikaye", "title": "Dört Hikaye", "author": "Anton Çehov",
                         "category": "Roman", "cover_url": f"{base}/covers/dort_hikaye.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "İnsan ruhunun derinliklerine inen seçkin öyküler.", "duration": 2_880_000, "view_count": 1710},
                        {"id": "sample_polisiye_gloria", "title": "Sherlock Holmes: Gloria Scott", "author": "Arthur Conan Doyle",
                         "category": "Polisiye", "cover_url": f"{base}/covers/sherlock_gloria.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Sherlock Holmes'un üniversite yıllarındaki ilk gizemli vakası.", "duration": 2_140_000, "view_count": 2768},
                        {"id": "sample_polisiye_kizil_dosya", "title": "Sherlock Holmes: Kızıl Dosya", "author": "Arthur Conan Doyle",
                         "category": "Polisiye", "cover_url": f"{base}/covers/kizil_dosya.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Baker Street dedektifinin Doktor Watson ile ilk büyük buluşması.", "duration": 4_860_000, "view_count": 1940},
                        {"id": "sample_polisiye_sussex_vampiri", "title": "Sussex Vampiri", "author": "Arthur Conan Doyle",
                         "category": "Polisiye", "cover_url": f"{base}/covers/sussex_vampiri.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Batıl inançların ardındaki gerçeği arayan esrarengiz soruşturma.", "duration": 1_850_000, "view_count": 1320},
                        {"id": "sample_polisiye_gizemli_kimlik", "title": "Gizemli Kimlik", "author": "Arthur Conan Doyle",
                         "category": "Polisiye", "cover_url": f"{base}/covers/gizemli_kimlik.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "İzler ve tanıklıklar arasında çözülmesi beklenen bir sır.", "duration": 1_620_000, "view_count": 1180},
                        {"id": "sample_turk_karpuz_sergisi", "title": "Karpuz Sergisi", "author": "Ahmet Rasim",
                         "category": "Türk Edebiyatı", "cover_url": f"{base}/covers/karpuz_sergisi.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "İstanbul sokaklarından sıcak ve ince mizah dolu anlatılar.", "duration": 1_950_000, "view_count": 2098},
                        {"id": "sample_turk_kirlangiclar", "title": "Kırlangıçlar", "author": "Ömer Seyfettin",
                         "category": "Türk Edebiyatı", "cover_url": f"{base}/covers/kirlangiclar.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Türk öykücülüğünün zarif ve sürükleyici klasiklerinden biri.", "duration": 1_740_000, "view_count": 1845},
                        {"id": "sample_dunya_hayvan_ciftligi", "title": "Hayvan Çiftliği", "author": "George Orwell",
                         "category": "Dünya Edebiyatı", "cover_url": f"{base}/covers/hayvan_ciftligi.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Güç, özgürlük ve toplum düzeni üzerine unutulmaz taşlama.", "duration": 10_800_000, "view_count": 2540},
                        {"id": "sample_dunya_dorian_gray", "title": "Dorian Gray'in Portresi", "author": "Oscar Wilde",
                         "category": "Dünya Edebiyatı", "cover_url": f"{base}/covers/dorian_gray.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Güzellik, vicdan ve tutku üzerine gotik başyapıt.", "duration": 21_600_000, "view_count": 2210},
                        {"id": "sample_gelisim_ozguven", "title": "Özgüven", "author": "Ralph Waldo Emerson",
                         "category": "Kişisel Gelişim", "cover_url": f"{base}/covers/ozguven.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Bireysel irade ve içsel bağımsızlık üzerine zamansız fikirler.", "duration": 3_600_000, "view_count": 2315},
                        {"id": "sample_gelisim_demir_irade", "title": "Demir İrade", "author": "Orison Swett Marden",
                         "category": "Kişisel Gelişim", "cover_url": f"{base}/covers/demir_irade.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Azim, disiplin ve kararlılıkla hedefe ulaşma rehberi.", "duration": 4_200_000, "view_count": 1765},
                        {"id": "sample_cocuk_kucuk_prens", "title": "Küçük Prens", "author": "Antoine de Saint-Exupéry",
                         "category": "Çocuk Masalları", "cover_url": f"{base}/covers/kucuk_prens.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Dostluk, sevgi ve kalple görmek üzerine evrensel masal.", "duration": 5_100_000, "view_count": 2630},
                        {"id": "sample_cocuk_keloglan", "title": "Keloğlan Masalları", "author": "Türk Halk Masalları",
                         "category": "Çocuk Masalları", "cover_url": f"{base}/covers/keloglan.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Anadolu kültürünün neşeli ve bilge halk masalları.", "duration": 1_920_000, "view_count": 1540},
                        {"id": "sample_cocuk_andersen", "title": "Andersen Masalları", "author": "Hans Christian Andersen",
                         "category": "Çocuk Masalları", "cover_url": f"{base}/covers/andersen.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Hayal gücünü besleyen klasik dünya masalları seçkisi.", "duration": 2_400_000, "view_count": 1490},
                        {"id": "sample_ingilizce_pride", "title": "Pride and Prejudice", "author": "Jane Austen",
                         "category": "İngilizce Kitaplar", "cover_url": f"{base}/covers/pride_prejudice.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Classic English novel of manners, wit, and romance.", "duration": 28_800_000, "view_count": 1985},
                        {"id": "sample_ingilizce_sherlock", "title": "The Adventures of Sherlock Holmes", "author": "Arthur Conan Doyle",
                         "category": "İngilizce Kitaplar", "cover_url": f"{base}/covers/sherlock_en.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Twelve iconic detective mysteries in original English.", "duration": 25_200_000, "view_count": 1810},
                        {"id": "sample_klasik_dr_jekyll", "title": "Dr. Jekyll ve Mr. Hyde", "author": "Robert Louis Stevenson",
                         "category": "Klasik", "cover_url": f"{base}/covers/dr_jekyll.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "İnsan doğasındaki iyilik ve karanlık çatışmasının klasiği.", "duration": 7_200_000, "view_count": 2150},
                        {"id": "sample_klasik_tilsim", "title": "Tılsım", "author": "Walter Scott",
                         "category": "Klasik", "cover_url": f"{base}/covers/talisman.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Sadakat ve onur üzerine örülü klasik serüven anlatısı.", "duration": 18_000_000, "view_count": 1410},
                        {"id": "sample_tarih_kandil", "title": "Kandil", "author": "Ömer Seyfettin",
                         "category": "Tarih", "cover_url": f"{base}/covers/kandil.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Geçmiş dönemlerden tarih ve insanlık manzaraları.", "duration": 1_580_000, "view_count": 1620},
                        {"id": "sample_tarih_seyahat", "title": "Tarih Boyunca Seyahatnameler", "author": "Jules Verne",
                         "category": "Tarih", "cover_url": f"{base}/covers/alman_gocmen.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Tarihsel keşif rotalarını ve yolculuk hikâyelerini bir araya getirir.", "duration": 4_900_000, "view_count": 1380},
                        {"id": "sample_felsefe_fil_maymun", "title": "Fil ve Maymun: Bilgelik Öyküleri", "author": "Ezop",
                         "category": "Felsefe", "cover_url": f"{base}/covers/fil_maymun.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Kısa fabllar eşliğinde düşünce, erdem ve yaşam bilgeliği.", "duration": 1_200_000, "view_count": 1470},
                        {"id": "sample_felsefe_kendine_guven", "title": "Kendine Güven Üzerine", "author": "Ralph Waldo Emerson",
                         "category": "Felsefe", "cover_url": f"{base}/covers/ozguven.jpg", "audio_url": f"{base}/audio.mp3",
                         "description": "Ahlak, akıl ve bireysel sorumluluk üzerine felsefi metinler.", "duration": 3_100_000, "view_count": 1290},
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
