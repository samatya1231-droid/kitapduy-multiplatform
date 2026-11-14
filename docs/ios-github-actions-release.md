# Mac Olmadan Windows Üzerinden App Store Connect / TestFlight Yayınlama (GitHub Actions)

Bu rehber, Codemagic kotası dolduğunda **GitHub Actions** (`https://github.com/samatya1231-droid/kitapduy-multiplatform`) üzerinden Windows bilgisayar kullanarak iOS `.ipa` derleme, imzalama ve Apple App Store Connect / TestFlight'a gönderme adımlarını açıklar.

---

## 1. Gerekli Apple İmzalama Dosyaları (Windows'ta Üretme)

İmzalama dosyalarını Git dışında, yalnızca erişebildiğiniz bir klasörde saklayın. Örneklerde `apple-signing` klasörü kullanılıyor. Daha önce oluşturulmuş bir CSR kullanıyorsanız `.p12` üretirken aynı CSR’a ait özel anahtarı kullanın.

### A) Apple Distribution Sertifikası (`.p12`)
Elinizde geçerli bir **Apple Distribution** `.p12` dosyası ve parolası varsa B adımına geçin. Sertifikayı Codemagic oluşturduysa **Team settings → Code signing identities → iOS certificates** bölümünde indirme seçeneğini kontrol edin. [Codemagic imzalama kılavuzu](https://docs.codemagic.io/yaml-code-signing/signing-ios/)

`.p12` dosyasını alamıyorsanız Windows'taki Git OpenSSL ile sertifika isteği (CSR) hazırlayın.

1. **PowerShell** açın ve güvenli bir klasör oluşturun:
   ```powershell
   $signDir = "$env:USERPROFILE\apple-signing"
   New-Item -ItemType Directory -Force -Path $signDir | Out-Null
   Set-Location $signDir
   $ossl = "C:\Program Files\Git\mingw64\bin\openssl.exe"
   ```
   *(Not: `usr\bin\openssl.exe` yerine mutlaka `mingw64\bin\openssl.exe` kullanın; `-legacy` modülü onda bulunur.)*

2. Özel anahtar (`.key`) ve Apple CSR dosyası oluşturun:
   ```powershell
   & $ossl genrsa -out ios_distribution.key 2048
   & $ossl req -new -key ios_distribution.key `
     -out CertificateSigningRequest.certSigningRequest `
     -subj "/CN=KitapDuy Distribution/C=TR"
   ```

3. **Apple Developer Portal** → **Certificates, Identifiers & Profiles** → **Certificates** → **+** → **Apple Distribution** seçin.
   - `CertificateSigningRequest.certSigningRequest` dosyasını yükleyin ve oluşan **`distribution.cer`** dosyasını `$signDir` klasörüne indirin.

4. İndirilen `.cer` dosyasını PEM'e çevirip macOS uyumlu (legacy PKCS#12) `.p12` paketine dönüştürün (sizden güçlü bir `.p12` parolası isteyecektir — bu parolayı `IOS_P12_PASSWORD` olarak kaydedin):
   ```powershell
   & $ossl x509 -in distribution.cer -inform DER -out distribution.pem -outform PEM
   & $ossl pkcs12 -export -legacy -inkey ios_distribution.key -in distribution.pem -out ios_distribution.p12
   ```

### B) App Store Provisioning Profile (`.mobileprovision`)
1. **Apple Developer Portal** → **Identifiers** → `com.semtinazizi.kitapduy` kaydında **Push Notifications** ve **Sign in with Apple** yetkilerinin açık olduğunu doğrulayın.
2. **Profiles** → **+** → **Distribution → App Store Connect** seçin.
3. App ID olarak `com.semtinazizi.kitapduy`, sertifika olarak **A adımındaki Apple Distribution sertifikanızı** seçin.
4. Profil adı verin (ör. `Kitapduy Yayinlama`) ve indirilen `.mobileprovision` dosyasını `$signDir\kitapduy.mobileprovision` olarak kaydedin.
   *(Yeni sertifika oluşturduysanız eski profili mutlaka Edit/Regenerate edip yeniden indirin; sertifika eşleşmezse CI ilk adımda uyarır.)*

### C) App Store Connect API Key (`.p8`)
1. **App Store Connect** → **Users and Access** → **Integrations** → **App Store Connect API** (**Team Keys**) sekmesine gidin.
2. Elinizde yükleme yetkisi olan `AuthKey_XXXXXXXXXX.p8` dosyası, **Key ID** ve **Issuer ID** varsa bunları kullanın. Yeni bir Team Key gerekiyorsa **Generate API Key** seçeneğinde `Developer` rolünü seçin. [Apple yükleme yetkileri](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/)
3. `.p8` dosyası **yalnızca 1 kez** indirilebilir; `$signDir` klasörüne indirin, **Key ID** ve **Issuer ID** değerlerini not edin.

---

## 2. GitHub Secrets Ayarlama

`https://github.com/samatya1231-droid/kitapduy-multiplatform/settings/secrets/actions` sayfasında **New repository secret** ile şu 6 gizli anahtarı ekleyin:

| Secret Adı | Değer (PowerShell ile panoya kopyalama) |
|---|---|
| `IOS_BUILD_CERTIFICATE_BASE64` | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\apple-signing\ios_distribution.p12")) \| Set-Clipboard` |
| `IOS_P12_PASSWORD` | `.p12` oluştururken belirlediğiniz parola |
| `IOS_PROVISION_PROFILE_BASE64` | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\apple-signing\kitapduy.mobileprovision")) \| Set-Clipboard` |
| `APP_STORE_CONNECT_API_KEY_ID` | 10 karakterli API Key ID (örn. `ABC123DEF4`) |
| `APP_STORE_CONNECT_ISSUER_ID` | App Store Connect Issuer ID (UUID) |
| `APP_STORE_CONNECT_API_KEY_BASE64` | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\apple-signing\AuthKey_KEYID.p8")) \| Set-Clipboard` |

*(Not: `APP_STORE_CONNECT_API_KEY_BASE64` içine `.p8` dosyasının Base64 halini ya da ham PEM metnini yapıştırabilirsiniz; CI her ikisini de destekler.)*

---

## 3. Apple'a Yeni Build Gönderme

1. GitHub'da **Actions** → **KitapDuy iOS App Store Release** → **Run workflow** açın.
2. Alanlar:
   - `build_number`: İlk GitHub yüklemesinde **App Store Connect → TestFlight → Build Uploads** bölümündeki son build numarasından büyük bir değer girin. Alan boşsa akış, `Info.plist` build numarasına GitHub çalıştırma sırasını ekler. Daha önce elle daha yüksek bir numara kullandıysanız sonraki çalıştırmada da daha yüksek bir değer girin.
   - `marketing_version`: Boş bırakırsanız `Info.plist` sürümü (`1.0.2`) korunur.
   - `upload_to_app_store`: Apple'a yüklemek için işaretleyin. Varsayılan çalıştırma imzalı IPA ve günlükleri GitHub’da hazırlar.
3. Tamamlandığında build **App Store Connect → TestFlight / iOS App** bölümünde işleme alınır. İmzalı `.ipa`, `dSYM` dosyaları ve derleme günlükleri workflow **Artifacts** bölümünde 3 gün saklanır.

Apple işlemeyi tamamladığında incelemeye göndermek istediğiniz build’i **Distribution → iOS sürümü → Build** bölümünden seçin. [Apple build seçme kılavuzu](https://developer.apple.com/help/app-store-connect/manage-builds/choose-a-build-to-submit)

GitHub Free, özel depolar için aylık 12 ABD doları işlem kredisi sağlar. Standart macOS çalıştırıcısının dakika ücreti 0,062 ABD dolarıdır. Kota başka işlerle paylaşılır. [GitHub Actions ücretleri](https://docs.github.com/en/billing/concepts/product-billing/github-actions)
