# kitapduy3 / KMP ekran eşleştirmesi

Güncelleme: 28 Eylül 2026. Referans: yerel kitapduy3 çalışma kopyası.
Bu rapor kod karşılaştırmasıdır; iOS ekran görüntüsü onayı değildir. Tüm ekranlar için birebir eşitlik henüz sağlanmadı.

## Bu çalışmadaki değişiklikler

- Profil: ProfileReaderComponents referanstan taşındı; kapak/kimlik, takip listeleri, kitaplık/paylaşım sekmeleri, raflar ve yazarlar eklendi. Sarı dekoratif bloklar kaldırıldı. Fotoğraf yükleme ve hata durumları bağlandı.
- Profil veri yüklemesi iptal edilebilir; başarısız istekler sahte sıfır istatistik olarak sunulmuyor. Aynı eserin farklı raf kayıtları korunuyor. Mağaza Premium durumu kendi profilinde dikkate alınıyor.
- Keşfet: Android türler/özel seçkiler bileşeni taşındı. Arama geri düğmesi sorguyu temizliyor.
- Tema: ana sayfa başlık/mood kontrastı, podcast liste/oynatıcı renkleri, Ayarlar içindeki altı tema seçimi, Türkçe metin düzeltmeleri.
- Kitap: merkezde kapak, yazar ve oynatıcı kartı düzeni. Kitap/podcast atlamaları 15 saniye; başka içerik açıkken kaydırma devre dışı.
- Premium: plan seçimi ve tek satın alma düğmesi, yarar kartları. Mağaza fiyatı/geri yükleme korunur. iOS ücretsiz deneme kampanyası yok.
- Makaleler: Zihin Kütüphanesi gerçek RSS kaynakları, kategori seçimi ve hata/yeniden deneme. Kaynak sayfası kendiliğinden açılmaz.
- Bildirimler: Akıştaki boş düğme Supabase bildirim listesine bağlandı. İlgili gönderi/profil/kitap bağlantıları eklendi; canlı RLS doğrulanmadı.
- Analiz: sabit 450 dakika/12 kitap örnekleri kaldırıldı. Mevcut raf kayıtlarından tamamlanan kitap ve tür özeti gösterilir. Gerçek aylık süre/günlük aktivite henüz bağlı değil.
- Giriş: kullanılmayan onboarding son sayfasındaki boş giriş düğmeleri gerçek Welcome akışına yönlendirildi.
- Alıntı: işlevsiz paylaşım düğmesi yerine metin girip panoya kopyalama. Android ses/video alıntısıyla eşit değildir.

## Ekran kapsamı ve kalan farklar

| Ekran grubu | Durum / gereken sonraki kontrol |
|---|---|
| Main + Ana Sayfa, Keşfet, Akış, Kitaplık, Profil sekmeleri | Beşli gezinme mevcut. Keşfet ve profil düzenleri bu çalışmada taşındı; bütün durumlarda görsel eşitlik doğrulanmadı. |
| HomeScreen | Önceki taşıma + kontrast düzeltmeleri. Android ilk indirme yönlendirmesi/diğer kişiselleştirme ayrıntıları açık. |
| SearchScreen | Türler/özel seçkiler taşındı. Kitap talep et akışı eksik. |
| UserProfileScreen / ProfileEditScreen | Yeni temel düzen, fotoğraflar, raflar, takip listesi bağlandı. Rozetler eklendi. Anonim hesap uyarısı ve doğrudan profil paylaşım editörü eşitlik kontrolü açık. |
| SocialFeedScreen / PostDetailScreen | Çalışan gönderi/yorum akışları korunur. Yeni bildirim sayfası ve profil bağlantısı. Kulüp sekmesi/kurma, paylaşım modalinin tüm ayrıntıları eksik. |
| NotificationScreen | Liste/bağlantılar, görüntülenen bildirimleri okundu işaretleme ve göreli zaman eklendi. Canlı hesap/RLS kontrolü bekliyor. |
| LibraryTab / DownloadsScreen / PrideShelfScreen | Yerel indirme/geçmiş ve raflar mevcut. Android Firebase favorilerinin iOS hesabıyla eşleştirilmesi çözülmedi. |
| BookDetailScreen | Temel kapak/oynatıcı düzeni güncellendi. Benzer kitaplar eklendi; görsel/sesli alıntı paylaşımı ve tüm Android indirme etkileşimleri eşit değil. |
| PodcastListScreen / PodcastEpisodesScreen / PodcastDetailScreen | Liste/oynatıcı kontrastı ve seçili içeriğe bağlı seek düzeltildi. Ayrıntılı görsel doğrulama bekliyor. |
| CategoryDetailScreen / AuthorDetailScreen | Kaynak yapıları karşılaştırıldı; temel yapı benzer. Filtre ve büyük yazı/small-screen görsel kontrolü açık. |
| MoodDetailScreen | Başlık yüksekliği/adları ve yüzey kontrastı iyileştirildi; referans yerleşimi birebir taşınmadı. |
| MindLibraryScreen / ScienceArticlesScreen / ArticleDetailScreen | RSS içerik listeleri ve kaynakta okuma. Android tam metin okuma modları, cache ve reklamla açma akışı eşit değil. |
| SettingsScreen | Tema, Premium, bildirim, istatistik ve hesap işlemleri. Android destek/davet/yönetici girişleri tam karşılanmıyor. |
| SubscriptionPaywallScreen | Mağaza fiyatlı ücretli plan seçimi. Native StoreKit akışı korunur. Mağaza yönetim bağlantısı ve Android sticky checkout ayrıntıları eksik; ücretsiz deneme bilerek yok. |
| StatisticsScreen / PremiumWrappedScreen | Gerçek raf özeti mevcut. Android Firebase UserActivityRepository aylık dinleme/completion kayıtları KMP'ye bağlı değil; aynı aylık analiz denemez. |
| ClubsScreen / ClubDetailScreen | Yer tutucu ekranlar. Android reading_clubs ve joined_clubs Firestore koleksiyonları için doğrulanmış iOS kimlik/veri erişimi gerekiyor. |
| ChatScreen / AskidaSohbetScreen | Mevcut sohbet kodu korunur; Android teslim alma ve kulüp sohbeti eşitliği açık. |
| AskidaKitapScreen | Temel yapı benzer; teslim/şehir/tarih ayrıntıları ve iki tema görsel kontrolü açık. |
| KitapBilScreen / EntertainmentTab | Premium düğmesi bağlandı. Kart/özellik seti tam eşleşmiyor. |
| QuizScreen / GuessAuthorScreen / IndependentPensScreen | Bozuk Türkçe metinler düzeltildi. Turnuva/blitz/düello ve şiir akışları için ayrı işlevsel eşitlik çalışması gerekiyor. |
| FocusScreen | Tema kontrastı düzeltildi. Android istasyon/aktif kullanıcı gösterimi ve ambiyans seti eşit değil. |
| WelcomeScreen / LoginScreen / OnboardingScreen / SplashScreen | Mevcut Google/Apple/e-posta/anonim akış korunur; onboarding boş işlemleri düzeltildi. iOS native girişlerin canlı doğrulaması bekliyor. |
| LegalScreen | Platforma özel yerel yasal belge ekranı; web hedeflerinin birebir sunum eşitliği iddia edilmez. |

## 28 Eylül ek kontrolü

- Kaydırma ve atlama işlemleri oynatıcı katmanında beklenen kitap/podcast kimliğiyle doğrulanır. Eski ekrandan gelen olay, yeni içeriğe uygulanmaz; aynı kimlikli kitap/podcast ayrımı korunur.
- Art arda 15 saniye atlama native oynatıcı konumunu kullanır. Başlangıç/bitiş sınırları, reklam sırasında engelleme ve NaN girişi için dört ek test eklendi.
- Bildirim timestamp ayrıştırması ve göreli zaman için üç ek test eklendi. Geçersiz tarih az önce şeklinde gösterilmez.
- Profil düzenleme alanları gecikmeli yüklenen profile bağlandı; fotoğraf yenilemesi yazılan taslağı sıfırlamaz.
- Yerel HEAD çalışmaya başlarken c9c1226 idi (iOS build 21); önceki sürüm numarası değişikliği korunur.

## Doğrulama sınırları

- 28 Eylül: Android ortak Kotlin derlemesi ve 42 birim testi başarılı; hata/atlanan test yok. iOS Info.plist başlangıç doğrulaması ve git diff --check başarılı.
- Altı yeni ProfileScreenModel testi: oturumsuz kullanıcı, raf türlerinin korunması, hata/sıfır ayrımı, eski yüklemenin iptali, takip hatası ve takip listesi hatası.
- ADB cihaz listesi boş; bu oturumda çalışan Android emülatörü bulunamadı. iOS simülatörü yerelde yok.
- Codemagic main push tetikleyicisi yalnızca iOS simulator build kontrolünü çalıştırır. Bu rapordaki yerel testler yeni iOS build sonucunu veya görsel eşitliği doğrulamaz.
- Supabase şema/RLS ve Firebase UID–Supabase UID eşleştirmesi canlı doğrulanmadı. Yeni Firestore karşılıkları veya tablolar varsayılarak açılmadı.
- Ayrı Dede Korkut sadeleştirme isteği ayrı metin üretim paneline ait. Panelin kaynak projesi bu iki depoda bulunamadı; URL/klasör bilgisi bekleniyor. Eser veya ses dosyası değiştirilmedi.
