# Codemagic iPhone ve iPad kullanıcı senaryoları

Codemagic’te **iPhone and iPad User Journey Tests** (`ios-build-test`) akışını seçin. `main` dalına yapılan push bu akışı başlatır. Akış Release simülatör uygulamasını ve XCUITest hedefini derler. İki cihaz ailesini de destekleyen en yeni kurulu iOS çalışma ortamında bir iPhone ve bir iPad oluşturur. Cihaz ve iOS sürümü raporda gösterilir.

**Artifacts** bölümünden **`kitapduy-ios-qa.zip`** dosyasını indirin, açın ve içindeki **`index.html`** dosyasını tarayıcıda görüntüleyin. Galeri her cihazın videosunu, senaryo ekranlarını, hata ekranını, erişilebilirlik bulgularını ve günlüklerini bir araya getirir. `recording.mp4` ve PNG dosyaları da ayrı artifact olarak sunulur. `journey.xcresult.zip`, Xcode ile açılabilen ayrıntılı test sonucudur. `summary.json` hangi cihazın geçtiğini ve varsa eksik kanıtı gösterir.

## Kapsam

| Senaryo | Beklenen davranış |
|---|---|
| İlk açılış ve misafir girişi | Uygulama açılır. Misafir düğmesiyle Ana Sayfa’ya girilir ve kitaplar görünür. Ktor Darwin, ayrı bir HTTPS isteğinde başlıkları ve gövdeyi okuyabilir. |
| Arama ve kitap | Sonuçsuz arama boş durumu gösterir. Kitap sonucu açılır. Gerçek AVPlayer ilerlemesi, duraklatma ve 15 saniye ileri/geri kontrolleri doğrulanır. |
| Podcast ve Kitaplık | RSS bölüm listesi açılır ve podcast konumu ilerler. İndirilenler boş durumu ve Bitirdiklerim rafı açılır. |
| Akış ve Profil | Örnek gönderi yazılır ve Akış’ta görünür. Profil adı düzenlenir ve kaydedilen ad görünür. Gönderi ve profil güncellemesi yerel test sunucusunda da doğrulanır. |
| Ayarlar ve Premium | Koyu tema anahtarı değişir. Premium açılır. Geri yükleme düğmesi ve iki yasal ekran erişilebilir. |
| Erişilebilirlik | Apple denetimi seçili ekranlarda kontrast, kesilen yazı, dokunma alanı ve erişilebilirlik açıklaması bulgularını `accessibility.txt` dosyasına yazar. Bulgular inceleme için kaydedilir. |

Her cihaz kendi yerel test sunucusuyla başlar. Örnek kitap, podcast, hesap ve gönderiler simülatör senaryosuna aittir. HTTPS kontrolü varsayılan olarak `https://example.com/` kullanır. `IOS_SMOKE_HTTPS_URL` ile kimlik bilgisi içermeyen başka bir genel HTTPS uç noktası seçilebilir. Canlı Supabase yetkileri, Google/Apple girişi, StoreKit satın alma ve fiziksel cihaz davranışı ayrı doğrulama gerektirir. Galeri `kitapduy3` ile ekranları elle karşılaştırmak için kullanılabilir.

İlk cihaz başarısız olsa da ikinci cihaz çalıştırılır. Bir senaryo hata verdiğinde, test sonucu ve mevcut ekran/video dosyaları saklanır. Video veya senaryo ekranlarının eksikliği başarısızlık olarak raporlanır. App Store incelemeleri `ios-release` akışından ayrı yönetilir.

## Yerel doğrulama

Python testleri simülatör gerektirmez. İki test localhost üzerinde HTTP sunucusu açar.

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
