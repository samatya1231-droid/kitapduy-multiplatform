# iOS / kitapduy3 eşitleme durumu — 25 Eylül 2026

Referans: `../kitapduy3` çalışma ağacındaki Android kodu. Android referans projesinde değişiklik yapılmadı.

## Bu değişiklikte bağlanan akışlar

- iOS oynatıcı reklam / kitap / reklam sırasındaki öğeleri tamamlandıkça ilerletir. İlk reklam yüklenemezse kitap denenir. Oynatma başlamadan ses oturumu ve kilit ekranı komutları kurulur; komut hedefleri ve gözlemciler serbest bırakılır.
- Kitap ve podcast başlangıç konumu saklanan ilerlemeden alınır. Reklam süresi kitap ilerlemesini değiştirmez. Kontrolcü platformun oynatma durumunu izler.
- İndirme seçili kitabı hedefler; önce oynatma zorunluluğu yoktur. Aynı anda ikinci indirme reddedilir. Başarı/hata ekranda gösterilir.
- İndirilenler ekranı oynatıcıyla aynı FileDownloader üzerinden listeleme, silme ve oynatma yapar. iOS eski Documents kökündeki indirmeleri de okuyabilir. Saklanan başlık/kapak bilgisi sayesinde katalog yüklenmeden indirilenler listelenebilir.
- iOS indirmesi tüm MP3'ü belleğe almak yerine NSURLSession geçici dosyasını taşır; HTTP hatası başarı olarak kaydedilmez. İlerleme başlangıç/bitiş olarak gösterilir, byte bazlı yüzde henüz yoktur.
- Podcast arama sonuçları bölüm ekranına gider.
- Sosyal akış gerçek kullanıcı kimliğini kullanır. Gönderi beğenme, oylama, silme, profil açma, fotoğraf açma ve sesli gönderi oynatma bağlandı. Sunucu yazımı başarısızsa hata gösterilir; beğeni sayısı sahte yerel başarı ile artırılmaz.
- Android'in kullandığı posts alanları, reaction biçimi (`uid:reaction`) ve sayısal anket seçenek anahtarları korundu.
- Kitap yorumları posts tablosundan okunur ve aynı tabloya yazılır. Başarısız yazım yorum taslağını silmez.
- Kitaplığa ekleme gerçek user_bookshelves yazımı yapar. Kitaplık okumasındaki farklı tablo adı da user_bookshelves olarak düzeltildi.
- Rastgele kitap beğeni/dinleyici sayıları kaldırıldı. Kitap sayıları katalog kaydından okunur; doğrulanmış canlı dinleyici kaynağı yoksa sayı yerine çizgi gösterilir.

## İkinci düzeltme grubu

- Genel gönderiler için PostDetailScreen eklendi: yorum listeleme, gönderme, düzenleme, silme ve beğenme.
- Akış, profil ve gönderi detayı aynı InteractiveFeedCard bileşenini kullanır. Profil sahibinin kimliği oturumdaki kullanıcı kimliği yerine geçirilmez.
- Yorum yazarı repository içinde aktif oturumdan alınır. Düzenleme/silme sorguları hem post_id hem user_id ile sınırlandırılır; sunucu hiçbir kayıt döndürmezse başarı gösterilmez. RLS ayrıca canlı ortamda doğrulanmalıdır.
- Çift gönderim engellenir; hata durumunda taslak ve mevcut yorum korunur. Silinen gönderide eski içerik temizlenir ve tekrar deneme gösterilir.
- Yorum ekleme/silme sonrası posts.comment_count gerçek feed_comments sayısıyla yenilenmeye çalışılır. Sayaç yenilemesi hata verirse zaten kaydedilmiş yorum tekrar gönderilmez. Farklı cihazların eşzamanlı işlemlerinde tam tutarlılık için sunucu tarafında atomik sayaç gerekir.
- Gönderi türü, anket ve tepki eşlemesi akış/profil/detay arasında ortaklaştırıldı. Boş audio_url veya boş poll_options yanlış içerik türü üretmez.
- Şikayet/engelleme gerçek repository işlemlerine bağlandı; engellenen kullanıcıların gönderileri akış ve profil sorgusu sonuçlarından elenir.
- OneSignal kimlik aktarımı başlangıçta onay bekliyordu; kullanıcı 25 Eylül 2026 tarihinde açıkça onayladı. Aşağıdaki bildirim grubu uygulandı. Firebase UID ile Supabase UID otomatik birleştirilmedi.

## Kitaplık düzeltmeleri (25 Eylül 2026)

- Kitaplık geçmişi cihazdaki gerçek oynatma konumlarından, indirilenler FileDownloader kayıtlarından okunur. Bu iki sekme giriş gerektirmez; kayıtlı başlık/kapak ile çevrimdışı indirilen kitap açılabilir. Katalog zenginleştirmesi yerel listeyi bekletmez ve 8 saniye ile sınırlıdır.
- Kitaplık ve Gurur Rafı satırları kitap detayına bağlandı. Katalogda bulunamayan, indirilmemiş içerik için sahte veya boş ses adresiyle detay açılmaz.
- Kitap detayından READING, FINISHED ve WISHLIST raflarına ekleme bağlandı; aynı raflar kitaplıktan erişilebilir. Bu anahtarlar Android profilindeki raflarla eşleşir.
- Hızlı sekme geçişlerinde eski sorgu iptal edilir; raf okuma hataları boş liste olarak gizlenmez. Kitaplıkta tekrar deneme ve premium deneme süresini de dikkate alan hasPremium kontrolü eklendi.
- Android favorileri Firebase tabanlıdır. Önceki iOS FAVORITE raf sorgusu kaldırıldı; hesap/veri eşleştirmesi tamamlanana kadar açık eksiklik mesajı gösterilir. Favori eşitlemesi tamamlanmış sayılmaz.
- Beş yeni test: çevrimdışı/oturumsuz indirme, gerçek ilerleme ve eski geçmiş kayıtları, sekme yarışı, hata/tekrar deneme, yanlış favori rafı sorgusunun engellenmesi.

## iOS bildirim bağlantısı (25 Eylül 2026, kullanıcı onayı sonrası)

- Resmi OneSignal-XCFramework paketinin 5.7.0 sürümü ve OneSignalFramework ürünü Xcode hedefine bağlandı. Sürümün resmi Package.swift dosyası doğrulandı; paketin iOS minimumu uygulamadaki iOS 15 hedefiyle uyumludur.
- Android ile aynı public OneSignal App ID, Info.plist ONESIGNAL_APP_ID alanına eklendi. AppDelegate SDK'yı bir kez başlatır. REST API anahtarı veya APNs özel anahtarı uygulamaya eklenmedi.
- Gerçek Supabase oturumu UID'si OneSignal.login ile iletilir; gerçek çıkışta OneSignal.logout çağrılır. Başlangıç/yenileme ara durumları çıkış sayılmaz, aynı UID ile token yenilenmesi yinelenen login üretmez. Native SDK daha geç hazır olsa da son kesin oturum durumu aktarılır.
- Ayarlar > Bildirimler gerçek izin ve abonelik durumunu gösterir; kullanıcı buradan bildirimleri açıp kapatabilir. İzin ve abonelik gözlemcileri ile uygulamaya geri dönüş durumu günceller. İzin ilk açılışta zorla sorulmaz.
- APNs entitlement hem Debug hem Release imzalama ayarına bağlandı (development/production). Mevcut remote-notification arka plan modu korunur. Gizlilik metnine OneSignal kimlik/bildirim verisi aktarımı eklendi.
- Beş yeni test: belirsiz oturumun logout üretmemesi, gecikmiş SDK kurulumuna kimlik aktarımı, token yenileme/hesap geçişi/çıkış, SDK kurulmadan önce çıkış ve gerçek izin durumu gelmeden açık gösterilmemesi.
- Bu aşama temel push bağlantısıdır. Zengin görseller/teslim alındı analitiği için Notification Service Extension ve App Group eklenmedi.

### Yayın ve cihaz doğrulaması

1. OneSignal panelinde aynı App ID altında Settings > Push & In-App > Apple iOS (APNs) bağlantısını gerçek uygulamanın bundle ID, Apple Team ID ve APNs anahtarıyla doğrulayın. Bu panelde değişiklik veya gerçek push gönderimi yapılmadı.
2. Apple Developer uygulama kimliğinde Push Notifications yetkisini etkinleştirin; Codemagic/Xcode imzalama profilinin aps-environment yetkisini içerdiğini doğrulayın. Eski profil bu yetkiyi içermiyorsa yenilenmelidir.
3. Mac üzerinde Xcode paket çözümlemesi, derleme ve imzalama; ardından iPhone/TestFlight üzerinde izin verme/reddetme, ayarlardan açma/kapatma, arka plan teslimi ve A hesabından B hesabına geçiş denenmelidir. Windows testleri Swift derlemesini veya APNs teslimini doğrulamaz.
4. iOS external_id Supabase UID'sidir. Android'den yalnızca Firebase UID'ye gönderilen kişisel bildirimler, doğrulanmış sunucu kimlik eşleştirmesi olmadan iOS hesabına otomatik ulaşmaz. Hesaplar e-posta üzerinden istemcide birleştirilmedi.
5. OneSignal logout cihaz aboneliğini eski hesaptan ayırır; OneSignal tarafındaki kullanıcı kaydının tamamen silinmesi için hesap silme sunucu akışına ayrıca OneSignal silme API'si bağlanmalıdır. Sunucu gizli anahtarı istemciye konulmamalıdır.

Resmi kaynaklar: [iOS kurulum](https://documentation.onesignal.com/docs/en/ios-sdk-setup), [SDK kimlik ve abonelik API'leri](https://documentation.onesignal.com/docs/en/mobile-sdk-reference), [5.7.0 sürümü](https://github.com/OneSignal/OneSignal-iOS-SDK/releases/tag/5.7.0).

### Yeni kitap ve 22.00 bildirimleri

- iOS, Ana Sayfa ilk açıldığında henüz karar vermemiş kullanıcıdan sistem bildirim iznini ister. Kullanıcı daha sonra Ayarlar → Bildirimler üzerinden tercihini değiştirebilir.
- Kitap üreticisi, Android’e Firebase `all` konusundan gönderiyor. KitapDuy3 sunucusundaki `pushPublishedBookToIos` aynı yayını OneSignal üzerinden iOS’a iletir. Yalnızca kitap yayıncısının doğrulanmış hizmet hesabı olayı tetikleyebilir. Bildirim, kitap iOS kataloğundan okunabildikten sonra gönderilir.
- `pushEveningReminderToIos`, Türkiye saatiyle her gün 22.00’de OneSignal üzerinden iOS’a gönderir. Mevcut zamanlanmış bildirimin başlığını ve metnini kullanmak için `eveningTitle` ve `eveningBody` alanlarını doldurun. Aynı bildirimi iOS’a gönderiyorsa eski zamanlamayı devre dışı bırakın.
- Firebase Secret Manager’da `IOS_ONESIGNAL_PUSH_CONFIG` için `apiKey` (OneSignal App API Key), `segment` (doğrulanmış OneSignal hedef segmenti), `publisherAuthId` (Firestore CloudEvent’teki yayıncı hizmet hesabı kimliği), `supabaseAnonKey` (iOS uygulamasının herkese açık katalog anahtarı), `eveningTitle` ve `eveningBody` alanlarını içeren bir JSON tanımlayın. Firebase’e sadece bu iki fonksiyon yayımlanabilir. Önce iOS test cihazını içeren bir segment ve doğrulanmış OneSignal APNs bağlantısı ile teslimi sınayın.

## Oturum ve hesap silme hata yönetimi (26 Eylül 2026)

- Supabase çıkış hatası artık repository içinde yutulmaz. Başarılı çıkışta oturumun kapandığı kontrol edilir ve OneSignal kimliği temizlenir. Ayarlar ekranı hata gösterir; art arda çıkış istekleri ve eşzamanlı hesap silme engellenir. AuthViewModel başarısızlıkta yerel profil durumunu sahte olarak temizlemez.
- Hesap silme Edge Function'ındaki 15 tablo temizliğinin her sonucu kontrol edilir. Bir işlem hata verirse Auth hesabı silinmez ve başarı dönülmez; hata ayrıntıları loga yazılmaz. İşlemler atomik değildir, önceki adımların silmiş olduğu kayıtlar geri alınmaz.
- Altı yeni çevrimdışı Node sunucu testi başarılı; 28 mevcut Kotlin testi yeniden başarılı. Sunucu canlıya dağıtılmadı, gerçek kullanıcı verileri değiştirilmedi.
- İncelenen depolarda doğrulanmış Firebase–Supabase kimlik köprüsü bulunamadı. Canlı şema/RLS ve sahiplik raporu mevcut olmadığı için hesaplar otomatik eşleştirilmedi.

## Doğrulama

- `:composeApp:compileDebugKotlinAndroid`: başarılı. Ortak Kotlin/Compose kodunu ve Android actual uyumunu kontrol eder, iOS derlemesi değildir.
- `:composeApp:testDebugUnitTest` seçili altı test sınıfı: toplam 28 test, sıfır hata (6 oynatma/indirme, 6 gönderi ekranı, 3 sahte HTTP repository testi, 3 hesap silme, 5 kitaplık, 5 bildirim kimliği).
- Yeni testler: reklam sırasında konumun korunması, saklanan konumla başlatma, çevrimdışı reklam atlama, doğru kitabın indirilmesi, oynatmadan indirme ve hata, platformdan duraklatma durumunun okunması, zamanlayıcı bitişi/iptali.
- iOS AVFoundation çağrıları yerel Kotlin/Native 2.2.0 platform metadata tanımlarıyla kontrol edildi. Tam Xcode derlemesi, imzalama ve cihaz çalıştırması Windows'ta yapılmadı.
- Gerçek kullanıcı hesabıyla Supabase yazma/RLS veya canlı satın alma testleri yapılmadı.

## Tam eşitlik için kalan bağımlılıklar

1. **Kitap beğenileri ve görüntülenme sayaçları:** Android BookSocialViewModel, Firestore books/{id} ve books/{id}/likes/{uid} kullanıyor; iOS Supabase Auth kullanıyor. Aynı kullanıcı kimliği ve yetkilendirmeyi sağlayan doğrulanmış sunucu köprüsü gerekir. Yeni, bağımsız bir Supabase beğeni tablosu oluşturulmadı. iOS kitap beğenme şu anda açıklayıcı hata gösterir; yerel sahte başarı yoktur. Görüntülenme artırma ve canlı dinleyici sayısı tamamlanmadı.
2. **Push:** OneSignal SDK/kimlik bağlantısı, APNs entitlement ve izin yönetimi eklendi. OneSignal panelindeki APNs bağlantısı, imzalama profili ve gerçek cihaz teslimi henüz doğrulanmadı. Android tarafında doğrudan FCM tokenlarına gönderilen bildirimler için sunucu iOS dağıtımı ayrıca gerekir; APNs tokenı FCM tokenı gibi kaydedilmedi. Firebase/Supabase UID eşleştirmesi ve OneSignal sunucu hesabı silme bağlantısı da gereklidir.
3. **Canlı veri uyumu:** Aynı kişiye ait Firebase UID ve Supabase UID eşleşmesi ile posts / user_bookshelves RLS kuralları doğrulanmalı. İstemci kodunun derlenmesi bunu kanıtlamaz. Gönderi reaksiyonları Android'deki oku/güncelle modelini izler; farklı cihazların eşzamanlı güncellemeleri için sunucuda atomik işlem gerekir.
4. **Diğer sosyal özellikler:** Gönderi yorum ekranı ve profil etkileşimleri eklendi. Feed realtime halen önceki iOS stabilite tercihi nedeniyle kapalıdır. Yorum sayaçları ve reaksiyonlar için sunucuda atomik işlemler ve iki cihazla doğrulama gerekir.
5. **Cihaz kabul testi:** Ücretsiz/premium oynatma, reklamdan kitaba geçiş, yüklenirken duraklatma, kilit ekranı, arka plan sesi, uçak modunda indirilen kitap, yeniden başlatınca kaldığı yer, iki hesapla yorum/oylama/silme denenmeli.

Teknik referans: Apple AVPlayerItem status (https://developer.apple.com/documentation/avfoundation/avplayeritem/status-swift.property) ve MPRemoteCommand addTarget (https://developer.apple.com/documentation/mediaplayer/mpremotecommand/addtarget(handler:)).

## Tema ve ana ekran eşitlemesi (26 Eylül 2026)

- Güncel kitapduy3 Color.kt renkleri ve Theme.kt açık/koyu Material renk şemaları taşındı. Tema yöneticisinin eski koyu renkleri geri yüklemesi düzeltildi. Varsayılan görünüm Android gibi açık; kayıtlı tercihler korunur ve ayarlardaki koyu tema anahtarı anında uygulanır.
- Sistem Serif/SansSerif yerine paketlenmiş Inter ve Playfair Display türevi fontlar kullanılır. Gerçek kalınlıklar ve Türkçe karakterler doğrulandı, lisanslar uygulama kaynaklarına eklendi.
- Alt menü Android sırasına alındı: Ana Sayfa, Keşfet, Akış, Kitaplık, Profil. Etiketler sürekli görünür. Eğlence ve Askıda erişimleri ana sayfadaki kısa yollarda korunur. Yeni Keşfet kök ekranında işlevsiz geri düğmesi gösterilmez.
- Kitaplık Favoriler/İndirilenler/Geçmiş sekmeleri ve iki sütunlu kapak düzenine alındı. Diğer raflar Raflarım menüsünden açılır; raf verileri kaldırılmadı.
- Ana sayfanın ana sırası dinleme kartı, Yeni hikâyeler keşfet, yatay duygu kartları, Podcast dünyası ve kategoriler olarak güncellendi. Kitap kapakları Android'deki 136 dp genişlik ve 2:3 orana alındı. Sabit %45 ilerleme ve ilk kitabı geçmiş gibi gösterme kaldırıldı; kayıtlı konum veya açıkça ilk hikâye önerisi gösterilir.
- Bu çalışma tüm ekranların birebir görsel eşitliğini kanıtlamaz: kitap detayı, profil/akış, premium ekranları ve bazı alt bileşenlerde hâlâ ayrı uygulamalar vardır. İngilizce kitap bölümünün veri alanı eşleştirmesi ve bildirim rozeti gibi farklar kalır. Mac/iOS simülatörüyle açık/koyu tema, küçük ekran, büyük yazı ve çentik alanı kontrolü yapılmadı.
- Ortak Compose kodu Android hedefinde derlendi. Tam test paketinde 29 test başarılı; bu testler ekran görüntüsü karşılaştırması veya Swift/iOS derlemesi değildir.
