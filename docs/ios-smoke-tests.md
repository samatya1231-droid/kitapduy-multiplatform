# iPhone ve iPad kullanıcı senaryoları

GitHub’da **Actions → KitapDuy iOS QA → Run workflow** yolunu izleyip `main` dalını seçin. Bu akış elle başlatılır. GitHub artifact’i üç gün saklar. Codemagic’te **iPhone and iPad User Journey Tests** (`ios-build-test`) akışını seçin. `main` dalına yapılan push bu akışı başlatır.

Bitrise’ta özel GitHub deposunu projeye bağlayın. **Workflows → Change storage** üzerinden depodaki kök `bitrise.yml` dosyasını kullanın. **Start/Schedule a build** ekranında `main` dalı ve `ios-qa` akışını seçin. Arşiv, çalıştırmanın **Artifacts** bölümünde görünür.

Her akış Release simülatör uygulamasını ve XCUITest hedefini derler. İki cihaz ailesini de destekleyen en yeni kurulu iOS çalışma ortamında bir iPhone ve bir iPad oluşturur. Cihaz ve iOS sürümü raporda gösterilir.

Çalıştırmanın **Artifacts** bölümündeki **`kitapduy-ios-qa`** arşivini indirin, tamamını çıkarın ve **`index.html`** dosyasını tarayıcıda açın. Videolar sayfanın içinden izlenebilir. Sayfanın üst bölümündeki **Detaylı İnceleme Notu** panelinden (**Notu Panoya Kopyala** düğmesi veya **`inceleme-notu.md`** / **`inceleme-notu.txt`** dosyaları üzerinden) çalıştırma bulgularını doğrudan kopyalayıp paylaşabilirsiniz. Aynı ekranın iPhone ve iPad görüntüleri yan yana gelir ve tıklandığında tam boy açılır. Senaryo sonuçları, hata anları, çalışma zamanı denetim notları (`probes.txt` / `probes.json`) ve Apple erişilebilirlik bulguları (`accessibility.txt`) aynı raporda yer alır. `summary.json` cihaz başına senaryo sonuçlarını ve alınan kanıtları listeler. Codemagic ayrıca Xcode ile açılabilen `journey.xcresult.zip` arşivlerini saklar. Derleme simülatör testlerinden önce durursa GitHub ve Bitrise artifact’inde varsa derleme günlükleri yer alır.

## Kapsam (`test01` – `test14`)

| Senaryo | Beklenen davranış |
|---|---|
| İlk açılış ve misafir girişi (`test01`) | Uygulama açılır. Misafir düğmesiyle Ana Sayfa’ya girilir ve kitaplar görünür. Ktor Darwin, ayrı bir HTTPS isteğinde başlıkları ve gövdeyi okuyabilir. |
| Ana Sayfa, Keşfet ve kitap detayı (`test01`, `test07`) | Önerilen kitaplar, podcast sırası, kategori/yazar/ruh hali detayları ve makale görünümü kontrol edilir. Boş ve dolu arama sonuçları, kitap detayı ve aramaya dönüş doğrulanır. |
| Kitap ve podcast oynatma (`test02`, `test03`, `test10`) | Gerçek AVPlayer ilerlemesi, kitapta duraklatma ve 15 saniye ileri/geri kontrolleri, 1 Dakikada Eser Özeti, beğeni, hız/uyku/alıntı diyalogları, rafa ekleme, yorum gönderimi, Mini Player ve podcast oynatıcı kontrolleri denetlenir. |
| Edebi Danışman · derin inceleme (`test08`, `test09`) | Ana Sayfa FAB, Keşfet ve Kitaplık giriş noktaları; Hakkında ve Okuma Görünümü diyalogları; ruh hali chipleri, öneri/analiz/beyin fırtınası/münazara/okuma rotası akışları; derinlik ve spoiler ayarları; serbest soru, takip sorusu yönlendirmesi ve önerilen kitaptan `BookDetailScreen` geçişi denetlenir. |
| Kitap Talep Et, okur profilleri ve dolu Kitaplık (`test04`, `test11`) | Boş/dolu Kitaplık (Favoriler, İndirilenler, Geçmiş, raflar, Övünç Rafı), Kitap Talep Et formu, okur araması ve başka okur profilinde Takip et/Takipten çık akışları kontrol edilir. |
| Akış, anket, yorum CRUD ve bildirimler (`test05`, `test12`) | Gönderi/anket yazma, anket oylama, `PostDetailScreen` yorum ekleme/düzenleme/silme, bildirimlerde tümünü okundu işaretleme ve yerel test sunucusunda backend yazmaları doğrulanır. |
| Profil, Yıllık Özet, Arena ve Askıda Kitap (`test08`, `test13`) | Okur köşem, Yıllık Özet (`PremiumWrappedScreen`), Paylaşımlarım, profil düzenleme, KitapDuy Arena modları (Quiz, Turnuva, Blitz Arena, Liderlik) ve Askıda Kitap (arama, talep, sohbet, bağış) incelenir. |
| Koyu tema tam denetim ve Premium (`test06`, `test14`) | Koyu tema anahtarı, tüm ana ekranlarda (Ana Sayfa, Edebi Danışman, Keşfet, Kitap Detayı, Akış) koyu tema kontrast/yerleşim denetimi, Premium planları, geri yükleme ve iki yasal ekran doğrulanır. |
| Erişilebilirlik ve İnceleme Notu | Apple denetimi bulgularını `accessibility.txt` dosyasına, çalışma zamanı prob kontrollerini `probes.json` / `probes.txt` dosyalarına yazar; tümü **`inceleme-notu.md`**, **`inceleme-notu.txt`** ve `index.html` içinde özetlenir. |

Her cihaz kendi yerel test sunucusuyla başlar. Örnek kitap, podcast, hesap ve gönderiler simülatör senaryosuna aittir. HTTPS kontrolü varsayılan olarak `https://example.com/` kullanır. `IOS_SMOKE_HTTPS_URL` ile kimlik bilgisi içermeyen başka bir genel HTTPS uç noktası seçilebilir. Canlı Supabase yetkileri, Google/Apple girişi, StoreKit satın alma ve fiziksel cihaz davranışı ayrı doğrulama gerektirir. Galeri `kitapduy3` ile ekranları elle karşılaştırmak için kullanılabilir.

`ci/kitapduy3-reference` içindeki eşleşen PNG'ler **KitapDuy3 · Android** sütununa eklenir. `source.txt` referans sürümü ve veri bağlamını belirtir. Referansı güncellerken aynı ekran adlarını kullanın. Yerelde farklı bir dizin için `--android-reference` kullanılabilir.

İlk cihaz başarısız olsa da ikinci cihaz çalıştırılır. Örneğin podcast oynatması başarısızsa Kitaplık, Akış, Profil ve Premium senaryoları çalışmaya devam eder. Başarısız testler CI sonucunu etkiler ve mevcut ekran/video dosyaları yayımlanır. Rapor görüntüsü alınamayan adımları işaretler. App Store incelemeleri `ios-release` akışından ayrı yönetilir.

## Yerel doğrulama

Python testleri simülatör gerektirmez. Sunucu testleri localhost üzerinde HTTP sunucusu açar.

```sh
python3 -m unittest discover -s ci -p 'test_*.py'
```

Gerçek arayüz testleri için macOS ve Xcode gerekir. `codemagic.yaml` içindeki `KitapDuyUI` şemasıyla `build-for-testing` çalıştırdıktan sonra oluşan `.xctestrun` dosyasını kullanın.

```sh
python3 ci/run-ios-smoke.py \
  --app build/ios-simulator/Build/Products/Release-iphonesimulator/KitapDuy.app \
  --test-run path/to/KitapDuyUI.xctestrun \
  --output build/ios-smoke
```
