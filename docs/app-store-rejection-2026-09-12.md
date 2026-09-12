# App Store Red Notu - 12 Eylül 2026 (Submission fdc00670-27d1-49cf-9b08-60da5404a62e)

## Apple'ın bildirdiği sorunlar
1. Guideline 2.1(a) - "Sign in with Apple and Google" sırasında hata alındı, uygulamaya girilemedi.
2. Guideline 2.1(a) - "Keşfetmeye Başla" (misafir girişi) butonuna basınca hata oluştu.
3. Guideline 2.1(a) - Information Needed: App Review'a tüm özellikleri gösterecek bir demo hesap (kullanıcı adı/şifre) sağlanmamış.

## Kök neden analizi (ekran görüntülerinden)

Gönderilen 3 ekran görüntüsü incelendi:

- **Ekran 1**: `hojyvyuoafjogefhtrgg.supabase.co` üzerinde ham JSON hata görüntüleniyor:
  `{"code":400,"error_code":"validation_failed","msg":"Unsupported provider: provider is not enabled"}`
  Bu, Supabase projesinde **Apple** (veya denenen sağlayıcı) provider'ının Authentication ayarlarında
  etkinleştirilmediğini/gerekli anahtarların girilmediğini gösterir. Uygulama tarafı kodu
  (`SupabaseAuthRepository.getAppleOAuthUrl`) doğru URL'i talep ediyor; sorun sunucu tarafı konfigürasyonu.

- **Ekran 2**: Google ile girişte `accounts.google.com` şu hatayı veriyor:
  `Error 400: redirect_uri_mismatch`
  Bu hata Google Cloud Console'daki OAuth 2.0 Client'ın "Authorized redirect URIs" listesinde
  Supabase'in callback adresinin (`https://hojyvyuoafjogefhtrgg.supabase.co/auth/v1/callback`)
  kayıtlı olmadığını gösterir. Uygulama kodu `kitapduy://login-callback` şemasını sadece
  Supabase'in kendi callback'inden SONRA kullanıyor (`redirectUrl` parametresi); Google'a giden
  `redirect_uri` her zaman Supabase'in kendi callback URL'idir ve bu adresin Google Console'da
  kayıtlı olması gerekir.

- **Ekran 3**: "Misafir Olarak Keşfet" akışında görülen "Misafir girişi tamamlanamadı..." mesajı,
  uygulamanın (`AuthViewModel.signInAnonymously`) zaten var olan hata yakalama/kullanıcıya bilgi verme
  mantığından geliyor. Alttaki gerçek neden büyük ihtimalle Supabase projesinde
  **Anonymous Sign-Ins** ayarının kapalı olması (varsayılan olarak kapalıdır).

**Sonuç: Bu üç hata da uygulama kodundaki bir bug değil, Supabase / Google Cloud Console tarafındaki
eksik konfigürasyondan kaynaklanıyor.** Kod, hataları doğru şekilde yakalayıp kullanıcıya gösteriyor;
ancak sağlayıcılar sunucu tarafında etkin/doğru yapılandırılmadığı için giriş hiçbir yöntemle
tamamlanamıyor ve reviewer uygulamaya giremiyor.

## Yapılması gerekenler (dashboard tarafı - kod dışı)

### 1) Supabase → Authentication → Providers → Anonymous Sign-Ins
- "Allow anonymous sign-ins" seçeneğini AÇIN.
- Bu, misafir/"Keşfetmeye Başla" akışını çalışır hale getirir.

### 2) Supabase → Authentication → Providers → Google
- Provider'ın **Enabled** olduğunu doğrulayın.
- Client ID / Client Secret'ın Google Cloud Console'daki OAuth 2.0 Client ile eşleştiğini doğrulayın.
- Google Cloud Console → APIs & Services → Credentials → ilgili OAuth 2.0 Client ID → Authorized redirect URIs:
  `https://hojyvyuoafjogefhtrgg.supabase.co/auth/v1/callback`
  adresini ekleyin (yoksa bu yüzden `redirect_uri_mismatch` alınıyor).

### 3) Supabase → Authentication → Providers → Apple
- Provider'ı **Enabled** yapın.
- Apple Developer hesabından: Services ID, Team ID, Key ID ve .p8 private key ile Supabase'e
  Client ID/Secret bilgilerini girin (Supabase "Sign in with Apple" kurulum dokümanına göre).
- Apple Developer → Certificates, Identifiers & Profiles → ilgili Services ID → Sign in with Apple
  yapılandırmasında **Return URLs** alanına da
  `https://hojyvyuoafjogefhtrgg.supabase.co/auth/v1/callback`
  eklenmelidir.

### 4) App Store Connect → App Review Information
- "Sign-in required" işaretliyse bir **demo hesap** (e-posta + şifre) girin. Video/demo modu tek başına
  yeterli değil, Apple açıkça kullanıcı adı/şifre istiyor.
- Öneri: E-posta ile kayıt akışından sabit bir test hesabı oluşturup (`reviewer@kitapduy.app` gibi)
  bilgilerini App Store Connect'teki notlara yazın. Yukarıdaki 3 madde düzeltilmeden reviewer'ın
  girebileceği tek yol zaten bu demo hesap olacaktır.

## Bu commit'te yapılan kod tarafı değişiklik
- Yeni submission için build numarası artırıldı: `CFBundleShortVersionString` 1.0.1 → 1.0.2,
  `CFBundleVersion` 19 → 20 (`iosApp/iosApp/Info.plist`).
- Kök neden ve düzeltme adımları bu doküman olarak eklendi.

Not: Yukarıdaki dashboard ayarları düzeltilmeden yeniden gönderim yapılırsa aynı ret tekrar alınır;
bu ayarlar repo dışında (Supabase/Google/Apple portallarında) elle yapılmalıdır.
